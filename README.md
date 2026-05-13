# Renko - Plateforme de Gestion des Actions de Charité

Bienvenue sur le dépôt officiel du projet **Renko**, une plateforme moderne et intuitive dédiée à la gestion d'associations caritatives, d'initiatives solidaires, et à la collecte de dons en ligne.

## 🌟 Fonctionnalités Principales

- **Espace Associations :** Inscription, création et gestion d'initiatives/campagnes de collecte de fonds.
- **Dons Sécurisés :** Intégration complète de **Stripe** pour les paiements en ligne et le rechargement de portefeuilles virtuels.
- **Tableau de Bord Administrateur :** Gestion centralisée des utilisateurs, des associations et validation des campagnes avec des statistiques.
- **Espace Utilisateur (Donateur) :** Profil personnel, historique des dons, et suivi de l'impact caritatif.
- **Design Moderne :** Interface premium utilisant la tendance *Glassmorphism*, avec un design responsive.

## 🛠️ Stack Technique

- **Backend :** Java 17+, Spring Boot 3 (Spring Web, Spring Security, Spring Data JPA, Spring Data MongoDB)
- **Bases de données (Architecture Hybride) :** 
  - **H2 Database** (Base de données relationnelle pour les utilisateurs et l'authentification)
  - **MongoDB** (Base de données NoSQL pour stocker l'historique des dons et les données flexibles)
- **Frontend :** Thymeleaf, HTML5, Vanilla CSS (Glassmorphism), JavaScript
- **Paiement :** API Stripe
- **Outils :** Maven

## 🚀 Installation & Lancement en local

1. **Prérequis :**
   - Java 17 ou supérieur installé.
   - Serveur MongoDB exécuté en local sur le port `27017`.
   - Clés d'API Stripe (Publique, Privée, et Webhook Secret).

2. **Configuration :**
   Ouvrez le fichier `src/main/resources/application.properties` et ajoutez vos clés Stripe de test :
   ```properties
   stripe.public.key=pk_test_votre_cle_publique
   stripe.secret.key=sk_test_votre_cle_secrete
   stripe.webhook.secret=whsec_votre_webhook_secret
   ```

3. **Lancement :**
   Ouvrez un terminal à la racine du projet et lancez la commande suivante :
   ```bash
   ./mvnw spring-boot:run
   ```
   *Sous Windows : `.\mvnw.cmd spring-boot:run`*

4. **Accès :**
   Ouvrez votre navigateur et accédez à : `http://localhost:8087`

## 🤝 Contribution
Ce projet a été réalisé dans le cadre d'un PFA/PFE (Projet de Fin d'Études). N'hésitez pas à forker le projet, ouvrir des *issues* ou soumettre des *pull requests*.
