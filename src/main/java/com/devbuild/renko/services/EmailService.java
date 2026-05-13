package com.devbuild.renko.services;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    @SuppressWarnings("unused")
    private final JavaMailSender mailSender;

    public void sendNotification(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("renko-charity@no-reply.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            // Désactivé pour le développement afin d'éviter les erreurs 500
            // mailSender.send(message);
            System.out.println("SIMULATION EMAIL envoyé à : " + to + " | Sujet : " + subject);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email : " + e.getMessage());
            // On ne bloque pas l'application si l'email échoue
        }
    }

    public void broadcastInitiativeUpdate(String initiativeTitle, String updateDetails) {
        // Dans une vraie application, on bouclerait sur tous les donateurs de cette initiative
        // Ici, pour l'exemple, nous simulons l'envoi à un administrateur ou une liste
        String body = "Mise à jour sur l'initiative : " + initiativeTitle + "\n\n" +
                      "Détails : " + updateDetails + "\n\n" +
                      "Merci pour votre soutien !\nL'équipe Renko Charity";
        
        System.out.println("Envoi d'une notification email pour l'initiative : " + initiativeTitle);
        System.out.println("Contenu du message : \n" + body);
    }
}
