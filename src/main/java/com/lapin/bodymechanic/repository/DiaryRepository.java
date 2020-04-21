package com.lapin.bodymechanic.repository;

import com.lapin.bodymechanic.domain.Diary;
import org.springframework.data.repository.CrudRepository;

public interface DiaryRepository extends CrudRepository<Diary, Long> {
}
