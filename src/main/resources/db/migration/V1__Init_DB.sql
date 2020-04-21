create sequence hibernate_sequence start 1 increment 1;

create table exercise (
    id int8 not null,
    exercise_name varchar(255),
    primary key (id)
);

create table m_diary (
    id int8 not null,
    number_of_train int8,
    reps int8,
    weight int8,
    user_id int8,
    exercise_id int8,
    primary key (id)
);


create table m_user (
    id int8 not null,
    activation_code varchar(255),
    active boolean not null,
    email varchar(255), password varchar(255),
    username varchar(255), primary key (id)
);

create table message (
    id int8 not null,
    filename varchar(255),
    tag varchar(255),
    text varchar(2048),
    user_id int8,
    primary key (id)
);


create table user_role (
    user_id int8 not null,
    roles varchar(255)
);

alter table if exists m_diary
add constraint diary_user_fr
foreign key (user_id) references m_user;

alter table if exists m_diary
add constraint diary_exercise_fk
foreign key (exercise_id) references exercise;

alter table if exists message
add constraint message_user_fk
foreign key (user_id) references m_user;

alter table if exists user_role
add constraint user_role_user_fk
foreign key (user_id) references m_user;