/**
 * Spring Data JPA repositories — the only Java types that issue SQL against MySQL.
 *
 * Each interface maps to one {@code @Table}. Spring Data derives queries from method
 * names or {@code @Query} JPQL. Callers live in {@code service.impl}; controllers
 * never autowire repositories.
 *
 * Hibernate {@code ddl-auto=update} owns schema evolution. There is no Flyway/Liquibase
 * changelog; column comments come from {@code @Comment} on the entity fields.
 *
 * Primary keys are UUID strings ({@code CHAR(36)}), so every repository is
 * {@code JpaRepository<Entity, String>}.
 */
package com.srikrishnanethi.agamotto.repositories;
