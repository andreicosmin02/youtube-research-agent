package com.youtube.research.repository;

import com.youtube.research.entity.Conversation;
import com.youtube.research.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByUser(User user);
}
