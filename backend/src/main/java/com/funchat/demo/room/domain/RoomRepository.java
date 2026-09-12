package com.funchat.demo.room.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    @Query("select r from Room r where r.roomType = :type or (r.roomType is null and :type = :legacyType)")
    Page<Room> findByType(@Param("type") RoomType type, @Param("legacyType") RoomType legacyType, Pageable pageable);
}
