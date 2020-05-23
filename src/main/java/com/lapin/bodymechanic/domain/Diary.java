package com.lapin.bodymechanic.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "m_diary")
public class Diary {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Getter
    @Setter
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    @Setter
    @Getter
    private User author;

    @Setter
    @Getter
    private Long numberOfTrain;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exercise_id")
    @Setter
    @Getter
    private Exercise exercise;

    @Setter
    @Getter
    private Long weight;

    @Setter
    @Getter
    private Long reps;

    public Diary() {
    }
}
