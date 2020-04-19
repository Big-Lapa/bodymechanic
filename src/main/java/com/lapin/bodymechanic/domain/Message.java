package com.lapin.bodymechanic.domain;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

@Entity
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Setter
    @Getter
    private Long id;

    @Setter
    @Getter
    @NotBlank(message = "Please foll the message")
    @Length(max = 2048, message = "Message too long (more than 2kB")
    private String text;

    @Setter
    @Getter
    @Length(max = 255, message = "Message too long (more than 255")
    private String tag;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    @Setter
    @Getter
    private User author;

    @Setter
    @Getter
    private String filename;

    public String getAuthorName() {
        return author != null ? author.getUsername() : "<none>";
    }

    public Message() {
    }

    public Message(String text, String tag, User user) {
        this.author = user;
        this.text = text;
        this.tag = tag;
    }

}
