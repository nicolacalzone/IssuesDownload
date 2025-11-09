package io.github.psgs.issuesdownload;

import com.opencsv.CSVWriter;
import io.github.psgs.issuesdownload.gui.GUI;
import org.kohsuke.github.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class IssuesDownload {

    public static void main(String[] args) {
        try {
            Config.loadConfiguration();
        } catch (IOException ex) {
            System.out.println("An IOException occurred while loading the configuration!");
            ex.printStackTrace();
        }
        GUI.main(args);
    }

    public static String saveIssues(String repoDetails, GHIssueState issueState) {
        String[] repoInfo = repoDetails.split("/");
        if (repoInfo.length != 2) {
            return "Invalid repository format. Use: owner/repo";
        }

        File outputFile = new File("issues.csv");
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile),
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END)) {

            // Write header
            String[] header = {"Id", "Title", "Creator", "Assignee", "Milestone", "State", "Body Text", "URL"};
            writer.writeNext(header);

            GitHub github = GitHub.connectUsingOAuth(Config.githubtoken);
            GHRepository repository = github.getUser(repoInfo[0]).getRepository(repoInfo[1]);

            int count = 0;
            for (GHIssue issue : repository.getIssues(issueState)) {
                String[] row = new String[8];

                row[0] = String.valueOf(issue.getNumber());
                row[1] = issue.getTitle() != null ? issue.getTitle() : "";
                row[2] = issue.getUser().getLogin();

                row[3] = issue.getAssignee() != null ? 
                         (issue.getAssignee().getName() != null ? issue.getAssignee().getName() : issue.getAssignee().getLogin()) 
                         : "";

                row[4] = issue.getMilestone() != null ? issue.getMilestone().getTitle() : "";

                row[5] = issue.getState().toString();

                // Body can have commas, newlines, quotes → OpenCSV escapes automatically
                row[6] = issue.getBody() != null ? issue.getBody() : "";

                row[7] = issue.getHtmlUrl().toString();

                writer.writeNext(row);
                count++;
            }

            System.out.println("Downloaded " + count + " issues to " + outputFile.getAbsolutePath());
            return "Download Complete! Saved " + count + " issues to issues.csv";

        } catch (IOException ex) {
            ex.printStackTrace();
            if (ex.getMessage() != null && ex.getMessage().contains("api.github.com")) {
                return "Cannot reach GitHub. Check internet or token.";
            }
            return "Error: " + ex.getMessage();
        }
    }
}
