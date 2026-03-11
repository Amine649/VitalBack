package com.veterinaire.formulaireveterinaire.service;

import com.veterinaire.formulaireveterinaire.DTO.BlogCreateDTO;
import com.veterinaire.formulaireveterinaire.DTO.BlogUpdateDTO;
import com.veterinaire.formulaireveterinaire.Enums.BlogType;
import com.veterinaire.formulaireveterinaire.Enums.Pet;
import com.veterinaire.formulaireveterinaire.entity.Blog;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface BlogService {
    Blog createBlog(BlogCreateDTO dto, MultipartFile pdfFile) throws IOException;
    List<Blog> getAllBlogs();
    List<Blog> getBlogsByType(BlogType type);
    Blog getBlogById(Long id);
    void deleteBlog(Long id) throws IOException;
    Blog updateBlog(Long id, BlogUpdateDTO dto, MultipartFile newPdf) throws IOException;

    List<Blog> getBlogsByPet(Pet pet);
}
