package com.youtube.research.repository;

import com.youtube.research.entity.Message;
import com.youtube.research.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Find all messages for a specific conversation
    List<Message> findByConversation(Conversation conversation);

    // Find messages by conversation, ordered by creation time
    List<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation);
}