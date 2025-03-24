package com.image_uploader_week6.image_uploader_week6.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.image_uploader_week6.image_uploader_week6.services.ImageService;


@Controller
@RequestMapping("/")
public class HomeController {
    @Autowired
    private ImageService imageService;

    @GetMapping("/")
    public String index(@RequestParam(value = "page", defaultValue = "0") int page, 
                       @RequestParam(value = "size", defaultValue = "5") int size,
                       Model model) {
        
        Map<String, Object> result = imageService.getImages(page, size);
        
        model.addAttribute("images", result.get("images"));
        model.addAttribute("totalPages", result.get("totalPages"));
        model.addAttribute("currentPage", result.get("currentPage"));
        model.addAttribute("hasNextPage", result.get("hasNextPage"));
        model.addAttribute("pageSize", size);
        
        return "index";
    }

    @GetMapping("/upload")
    public String addimage() {
        return "addimage";
    }

    @PostMapping("/upload")
    public String uploadFiles(@RequestParam("images") MultipartFile[] files, @RequestParam("description") String description, RedirectAttributes redirectAttributes) {
        try {
            String response = imageService.uploadMultipleFiles(files, description);
            if(response.equals("success")) {
                redirectAttributes.addFlashAttribute("message", "Successfully uploaded");
            }
            else if(response.equals("empty")) {
                redirectAttributes.addFlashAttribute("message", "Please select a file to upload");
                return "redirect:/upload";
            }
            else if(response.equals("max")) {
                redirectAttributes.addFlashAttribute("message", "Upload file. Max number of images is 5");
                return "redirect:/upload";
            }
            else if(response.equals("size")) {
                redirectAttributes.addFlashAttribute("message", "Max image size is 1mb");
                return "redirect:/upload";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Failed to upload file: " + e.getMessage());
            return "redirect:/upload";
        }
        
        return "redirect:/";
    }

    @PostMapping("/delete")
    public String deleteFile(@RequestParam("id") String url, RedirectAttributes redirectAttributes) {
        String objectKey = url.substring(url.indexOf("amazonaws.com/") + 14, url.indexOf("?"));
        // System.out.println(objectKey);
        String result = imageService.deleteImage(objectKey);
        if ("success".equals(result)) {
            redirectAttributes.addFlashAttribute("message", "Successfully deleted");
            return "redirect:/";
        } else {
            redirectAttributes.addFlashAttribute("message", "There was a problem while deleting the file");
            return "redirect:/";
        }
    }
    
}