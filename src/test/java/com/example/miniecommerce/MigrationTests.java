package com.example.miniecommerce;

import java.sql.*;
import java.math.BigDecimal;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MigrationTests {
    String url(String schema) {
        String base=System.getenv("GAMEHUB_TEST_MYSQL_URL");
        if(base==null || !base.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/gamehub_cart_tests\\?.*")) {
            throw new IllegalStateException("Use the disposable MySQL test script.");
        }
        return base.replace("/gamehub_cart_tests?","/"+schema+"?");
    }
    Flyway flyway(String schema,String target) {
        var config=Flyway.configure().dataSource(url(schema),"gamehub_audit",System.getenv("GAMEHUB_TEST_MYSQL_PASSWORD"))
                .baselineOnMigrate(false).cleanDisabled(true);
        if(target!=null)config.target(target);
        return config.load();
    }
    Connection connection(String schema) throws SQLException {
        return DriverManager.getConnection(url(schema),"gamehub_audit",System.getenv("GAMEHUB_TEST_MYSQL_PASSWORD"));
    }

    @Test void legacyFixtureMigratesWithoutLosingRecordsOrInventingOwnership() throws Exception {
        String schema="gamehub_migration_tests";flyway(schema,"1").migrate();
        try(var c=connection(schema);var s=c.createStatement()) {
            s.executeUpdate("INSERT INTO utilisateurs(id,nom,utilisateur_role) VALUES(1,'Historical fixture',1)");
            s.executeUpdate("INSERT INTO produits(id,nom,prix,quantite) VALUES(1,'Legacy decimal fixture',0.105,5)");
            s.executeUpdate("INSERT INTO commandes(id,utilisateur_id,montant,commandestatut) VALUES(1,1,0.105,1)");
            s.executeUpdate("INSERT INTO ligne_commandes(id,commandes_id,produit_id,prix_unitaire,quantite) VALUES(1,1,1,0.105,1)");
            s.executeUpdate("INSERT INTO paiements(id,commande_id,montant,statut_paiement) VALUES(1,1,0.105,1)");
        }
        flyway(schema,null).migrate();flyway(schema,null).validate();
        try(var c=connection(schema);var s=c.createStatement();var r=s.executeQuery(
                "SELECT p.prix,p.version,c.montant,c.checkout_key,u.guest_token_hash FROM produits p JOIN commandes c ON c.id=1 JOIN utilisateurs u ON u.id=1 WHERE p.id=1")) {
            assertTrue(r.next());assertEquals(new BigDecimal("0.11"),r.getBigDecimal(1));assertEquals(0,r.getLong(2));
            assertEquals(new BigDecimal("0.11"),r.getBigDecimal(3));assertNull(r.getString(4));assertNull(r.getString(5));
        }
    }

    @Test void incompatibleHistoryBlocksBeforeConvertingMoney() throws Exception {
        String schema="gamehub_migration_invalid";flyway(schema,"1").migrate();
        try(var c=connection(schema);var s=c.createStatement()){s.executeUpdate("INSERT INTO produits(nom,prix,quantite) VALUES('Invalid fixture',-1,1)");}
        assertThrows(org.flywaydb.core.api.FlywayException.class,()->flyway(schema,null).migrate());
        try(var c=connection(schema);var s=c.createStatement();var r=s.executeQuery("SELECT prix FROM produits")) {
            assertTrue(r.next());assertEquals(-1,r.getInt(1));
        }
    }

    @Test void nonemptyUnmanagedDatabaseIsNotAutomaticallyAdopted() throws Exception {
        String schema="gamehub_migration_unmanaged";
        try(var c=connection(schema);var s=c.createStatement()){s.execute("CREATE TABLE migration_probe(id INT PRIMARY KEY)");}
        assertThrows(org.flywaydb.core.api.FlywayException.class,()->flyway(schema,null).migrate());
        try(var c=connection(schema);var s=c.createStatement();var r=s.executeQuery("SHOW TABLES")) {
            assertTrue(r.next());assertEquals("migration_probe",r.getString(1));assertFalse(r.next());
        }
    }
}
