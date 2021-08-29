package ru.shvets.blog.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.shvets.blog.models.GlobalSettings;

@Repository
public interface SettingsRepository extends JpaRepository<GlobalSettings, Long> {
}