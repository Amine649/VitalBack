package com.veterinaire.formulaireveterinaire.DAO;

import com.veterinaire.formulaireveterinaire.Enums.BlogType;
import com.veterinaire.formulaireveterinaire.Enums.Pet;
import com.veterinaire.formulaireveterinaire.entity.Blog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlogRepository extends JpaRepository<Blog, Long> {
    List<Blog> findByType(BlogType type);


    List<Blog> findByPet(Pet pet);
}
