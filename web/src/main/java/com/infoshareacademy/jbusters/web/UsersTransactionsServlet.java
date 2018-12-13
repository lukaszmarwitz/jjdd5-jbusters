package com.infoshareacademy.jbusters.web;

import com.infoshareacademy.jbusters.data.DataLoader;
import com.infoshareacademy.jbusters.data.Transaction;
import com.infoshareacademy.jbusters.data.UploadFileFromUser;
import com.infoshareacademy.jbusters.freemarker.TemplateProvider;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = "/users-transactions")
@MultipartConfig(location = "/tmp"
        , fileSizeThreshold = 1024 * 1024
        , maxFileSize = 1024 * 1024 * 5
        , maxRequestSize = 1024 * 1024 * 5 * 5)
public class UsersTransactionsServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(UsersTransactionsServlet.class);
    private static final String TEMPLATE_NAME = "users-transactions";

    @Inject
    private TemplateProvider templateProvider;

    @Inject
    private UploadFileFromUser uploadFileFromUser;

    @Inject
    private DataLoader dataLoader;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");


        final PrintWriter writer = response.getWriter();
        final Part filePart = request.getPart("file");
        List<Transaction> usersTransactions = new ArrayList<>();
        Map<String, Object> model = new HashMap<>();

        Template template = templateProvider.getTemplate(
                getServletContext(),
                TEMPLATE_NAME);

        String fileName;
        try {
            File file = uploadFileFromUser.uploadFile(filePart);
            fileName = file.getName();
            Path path2 = Paths.get(System.getProperty("jboss.home.dir") + "/upload/" + fileName);


            try {
                usersTransactions = dataLoader.createTransactionList(Files.readAllLines(path2), true);
                LOG.info("Loading file with name {}", fileName);
            } catch (Exception e) {
                LOG.error("File loading error {}", e.getMessage());
            }


            model.put("flats", usersTransactions);
        } catch (Exception e) {

            String errorMasage = "You either did not specify a file to upload or are "
                    + "trying to upload a file to a protected or nonexistent "
                    + "location.";
            model.put("error", errorMasage);

            LOG.error("Error with loading file. {}", e.getMessage());
        } finally {
            try {
                template.process(model, writer);
                LOG.info("Loaded users flats. Number of flats: {}", usersTransactions.size());
            } catch (TemplateException e) {
                LOG.error("Failed to load users flats. Number of flats: {}", usersTransactions.size());
            }
            if (writer != null) {
                writer.close();
            }
        }
    }
}