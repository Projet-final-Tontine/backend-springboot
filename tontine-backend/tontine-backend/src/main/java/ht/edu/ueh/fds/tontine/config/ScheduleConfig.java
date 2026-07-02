package ht.edu.ueh.fds.tontine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Active l'automate temporel du systeme
 * (purge des comptes inactifs, rappels d'echeance).
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {
}
