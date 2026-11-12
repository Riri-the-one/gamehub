-- Baseline of the original schema. Empty databases only; never automatically baseline an existing database.
CREATE TABLE categories (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, nom VARCHAR(255), description VARCHAR(255)) ENGINE=InnoDB;
CREATE TABLE utilisateurs (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, nom VARCHAR(255), prenom VARCHAR(255), email VARCHAR(255), utilisateur_role TINYINT) ENGINE=InnoDB;
CREATE TABLE produits (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, nom VARCHAR(255), description VARCHAR(255), prix DOUBLE, quantite INT, image VARCHAR(255), categorie_id BIGINT,
 CONSTRAINT fk_product_category FOREIGN KEY(categorie_id) REFERENCES categories(id)) ENGINE=InnoDB;
CREATE TABLE paniers (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, date DATETIME(6), utilisateur_id BIGINT UNIQUE,
 CONSTRAINT fk_cart_user FOREIGN KEY(utilisateur_id) REFERENCES utilisateurs(id)) ENGINE=InnoDB;
CREATE TABLE ligne_paniers (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, quantite INT, paniers_id BIGINT, produits_id BIGINT,
 CONSTRAINT fk_cart_line_cart FOREIGN KEY(paniers_id) REFERENCES paniers(id),
 CONSTRAINT fk_cart_line_product FOREIGN KEY(produits_id) REFERENCES produits(id)) ENGINE=InnoDB;
CREATE TABLE commandes (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, date_commande DATETIME(6), commandestatut TINYINT, montant DOUBLE, adresse_de_livraison VARCHAR(255), utilisateur_id BIGINT,
 CONSTRAINT fk_order_user FOREIGN KEY(utilisateur_id) REFERENCES utilisateurs(id)) ENGINE=InnoDB;
CREATE TABLE ligne_commandes (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, quantite INT, prix_unitaire DOUBLE, commandes_id BIGINT, produit_id BIGINT,
 CONSTRAINT fk_order_line_order FOREIGN KEY(commandes_id) REFERENCES commandes(id),
 CONSTRAINT fk_order_line_product FOREIGN KEY(produit_id) REFERENCES produits(id)) ENGINE=InnoDB;
CREATE TABLE paiements (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, methode_paiement TINYINT, statut_paiement TINYINT, montant DOUBLE, date_paiement DATETIME(6), commande_id BIGINT,
 CONSTRAINT fk_payment_order FOREIGN KEY(commande_id) REFERENCES commandes(id)) ENGINE=InnoDB;
