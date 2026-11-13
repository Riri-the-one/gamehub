-- Preflight refuses incompatible historical data instead of deleting, fixing, or silently overflowing it.
DELIMITER //
CREATE PROCEDURE gamehub_v2_preflight()
BEGIN
 IF EXISTS (SELECT 1 FROM produits WHERE prix < 0 OR prix > 99999999999999999.99)
 OR EXISTS (SELECT 1 FROM commandes WHERE montant < 0 OR montant > 99999999999999999.99)
 OR EXISTS (SELECT 1 FROM ligne_commandes WHERE prix_unitaire < 0 OR prix_unitaire > 99999999999999999.99)
 OR EXISTS (SELECT 1 FROM paiements WHERE montant < 0 OR montant > 99999999999999999.99)
 OR EXISTS (SELECT commande_id FROM paiements WHERE commande_id IS NOT NULL GROUP BY commande_id HAVING COUNT(*) > 1)
 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Historical money/payment data requires manual review on a backup copy';
 END IF;
END//
DELIMITER ;
CALL gamehub_v2_preflight();
DROP PROCEDURE gamehub_v2_preflight;
ALTER TABLE produits MODIFY prix DECIMAL(19,2), ADD version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE commandes MODIFY montant DECIMAL(19,2), ADD checkout_key VARCHAR(64), ADD checkout_fingerprint VARCHAR(64), ADD CONSTRAINT uk_order_checkout UNIQUE(checkout_key);
ALTER TABLE ligne_commandes MODIFY prix_unitaire DECIMAL(19,2);
ALTER TABLE paiements MODIFY montant DECIMAL(19,2), ADD CONSTRAINT uk_payment_order UNIQUE(commande_id);
ALTER TABLE utilisateurs ADD guest_token_hash VARCHAR(64), ADD CONSTRAINT uk_guest_token UNIQUE(guest_token_hash);
