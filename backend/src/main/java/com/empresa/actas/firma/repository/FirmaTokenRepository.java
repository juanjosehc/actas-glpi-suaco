package com.empresa.actas.firma.repository;

import com.empresa.actas.firma.entity.FirmaToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FirmaTokenRepository extends JpaRepository<FirmaToken, Long> {

    Optional<FirmaToken> findByToken(String token);
}
