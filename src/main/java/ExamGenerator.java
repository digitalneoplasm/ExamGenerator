/*
ExamGenerator.java
Daniel R. Schlegel
7/20/2020

The purpose of this project is to overcome deficiencies in tools like Blackboard for giving exams remotely in
computer science. It automatically generates exams for each student from question banks, and supports sharing/unsharing
the exams with students given a class list.

Arguments:
- An $ExamFolder$, detailed below.
- One of:
-- generate - generates the exam documents for each student.
-- share - shares the respective document with each user until explicitly unshared.
-- share <time in minutes to share for> - shares the respective document with each user, then waits for the time to expire and unshares them.
-- share <username> - share only the exam for a specific user with them, until explicitly unshared.
-- unshare - unshare respective documents with each user.
-- unshare <username> - unshare only the exam for a specific user.

This program pre-supposes you have a folder structure as follows:
/$ExamFolder$/Q1
/$ExamFolder$/Q2
...
/$ExamFolder$/Qn
/$ExamFolder$/ClassList

where $ExamFolder$ is a folder name passed as an argument to this program. ClassList should be a Google Sheet of the
same format Banner provides when you choose to download the class list for a course as a CSV.
 */


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.DocsScopes;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.apache.commons.cli.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

public class ExamGenerator {
    private static final String APPLICATION_NAME = "Exam Generator";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE,DocsScopes.DOCUMENTS, SheetsScopes.SPREADSHEETS_READONLY);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = ExamGenerator.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private static Sheet getClassList(String examFolder){
        return null;
    }

    private static String getExamFolderId(String folderName, Drive driveService) throws IOException {
        String pageToken = null;
        FileList result = driveService.files().list()
                .setQ("name = '" + folderName + "' and mimeType = 'application/vnd.google-apps.folder'")
                .setSpaces("drive")
                .setFields("nextPageToken, files(id, name)")
                .setPageToken(pageToken)
                .execute();
        File examFolder = result.getFiles().get(0);
        System.out.printf("Found exam folder %s (%s)\n", examFolder.getName(), examFolder.getId());
        return examFolder.getId();
    }

    private static String getStudentExamFolderId(String examFolderId, Drive driveService) throws IOException {
        String pageToken = null;
        FileList result = driveService.files().list()
                .setQ("name = 'Student Exams' and parents = '" + examFolderId + "'")
                .setSpaces("drive")
                .setFields("nextPageToken, files(id, name)")
                .setPageToken(pageToken)
                .execute();
        File studentExamFolder = result.getFiles().get(0);
        return studentExamFolder.getId();
    }

    private static String getStudentExamId(Student student, String studentExamsFolderId, Drive driveService) throws IOException {
        String pageToken = null;
        FileList result = driveService.files().list()
                .setQ("name = '" + student.toString() + "' and parents = '" + studentExamsFolderId + "'")
                .setSpaces("drive")
                .setFields("nextPageToken, files(id, name)")
                .setPageToken(pageToken)
                .execute();
        File studentExam = result.getFiles().get(0);
        //System.out.printf("Found exam folder %s (%s)\n", examFolder.getName(), examFolder.getId());
        return studentExam.getId();
    }

    private static List<String> getQuestionFolderIDs(String examFolderID, Drive driveService) throws IOException {
        String pageToken = null;

        List<File> questionFolders = new ArrayList<>();

        // There could be lots of these.
        do {
            FileList result = driveService.files().list()
                    .setQ("name contains 'Q' and parents = '" + examFolderID + "' and mimeType = 'application/vnd.google-apps.folder'")
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name)")
                    .setPageToken(pageToken)
                    .execute();
            questionFolders.addAll(result.getFiles());
            pageToken = result.getNextPageToken();
        } while (pageToken != null);

        questionFolders.sort(Comparator.comparing(File::getName));
        questionFolders.forEach(f -> System.out.println("Found Question Folder: " + f.getName() + " (" + f.getId() +")"));

        return questionFolders.stream().map(File::getId).collect(Collectors.toList());
    }

    private static Exam buildExam(List<String> questionFolderIDs, Drive driveService) throws IOException {
        Exam exam = new Exam();

        for (String qf : questionFolderIDs){
            String pageToken = null;
            FileList result = driveService.files().list()
                    .setQ("parents = '" + qf + "'")
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name)")
                    .setPageToken(pageToken)
                    .execute();
            List<File> variantFiles = result.getFiles();

            Question question = new Question();
            for (File vf : variantFiles){
                question.addVariant(vf.getName(), vf.getId());
            }
            exam.addQuestion(question);
        }

        return exam;
    }

    private static String getClassListId(String examFolderID, Drive driveService) throws IOException {
        String pageToken = null;
        FileList result = driveService.files().list()
                .setQ("name = 'ClassList' and parents = '" + examFolderID + "'")
                .setSpaces("drive")
                .setFields("nextPageToken, files(id, name)")
                .setPageToken(pageToken)
                .execute();
        File examFolder = result.getFiles().get(0);
        System.out.printf("Found file %s (%s)\n", examFolder.getName(), examFolder.getId());
        return examFolder.getId();
    }

    private static List<Student> getStudents(String classListID, Sheets sheetsService) throws IOException {
        ValueRange response = sheetsService.spreadsheets().values()
                .get(classListID, "Sheet1!A1:K10000") // If you have more than 10,000 students... you have other problems.
                .execute();
        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
        } else {
            System.out.println("ID, LName, FName, Email");
            for (List row : values) {
                System.out.printf("%s, %s, %s, %s\n", row.get(0), row.get(1), row.get(2), row.get(7));
            }
        }

        List<Student> students = values.stream()
                .map(r -> new Student(r.get(0).toString(), r.get(1).toString(), r.get(2).toString(), r.get(7).toString()))
                .collect(Collectors.toList());

        return students;
    }

    public static void moveFile(String fileId, String folderId, Drive driveService) throws IOException {
        // Retrieve the existing parents to remove
        File file = driveService.files().get(fileId)
                .setFields("parents")
                .execute();
        StringBuilder previousParents = new StringBuilder();
        for (String parent : file.getParents()) {
            previousParents.append(parent);
            previousParents.append(',');
        }
        // Move the file to the new folder
        file = driveService.files().update(fileId, null)
                .setAddParents(folderId)
                .setRemoveParents(previousParents.toString())
                .setFields("id, parents")
                .execute();
    }

    public static File copyQuestionFile(String fileId, String fileName, String destFolderId, Drive driveService) throws IOException {
        File copiedFile = new File();
        copiedFile.setName(fileName);
        try {
            return driveService.files().copy(fileId, copiedFile).execute();
        } catch (IOException e) {
            System.out.println("An error occurred: " + e);
        }
        return null;
    }

    // This approach doesn't work in the current google docs API.
//    public static void buildStudentExamDoc(Student student, List<Question.QuestionVariant> variant, String studentExamFolderId, Docs docsService, Drive driveService) throws IOException{
//        Document doc = new Document().setTitle(student.toString());
//
//        Body body = new Body();
//        List<StructuralElement> elems = new ArrayList<>();
//
//        for (Question.QuestionVariant qv : variant){
//            Document qvDoc = docsService.documents().get(qv.getId()).execute();
//            elems.addAll(qvDoc.getBody().getContent());
//        }
//        body.setContent(elems);
//        doc.setBody(body);
//
//        doc = docsService.documents().create(doc).execute();
//        moveFile(doc.getDocumentId(), studentExamFolderId, driveService);
//    }

    public static void buildStudentExamFolder(Student student, List<Question.QuestionVariant> variant, String studentExamFolderId, Docs docsService, Drive driveService) throws IOException {
        File studentsWorkFolder = new File();
        studentsWorkFolder.setName(student.toString());
        studentsWorkFolder.setMimeType("application/vnd.google-apps.folder");
        studentsWorkFolder.setParents(Collections.singletonList(studentExamFolderId));
        studentsWorkFolder = driveService.files().create(studentsWorkFolder)
                .setFields("id")
                .execute();
        System.out.println("Built exam folder: " + student.toString()+ " (" + studentsWorkFolder.getId() + ")");

        for (Question.QuestionVariant qv : variant){
            File questionFile = copyQuestionFile(qv.getId(), qv.getName(), studentsWorkFolder.getId(), driveService);
            moveFile(questionFile.getId(), studentsWorkFolder.getId(), driveService);
        }
    }

    public static String createStudentExams(String examFolderID, Exam exam, List<Student> students, Drive driveService, Docs docsService) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName("Student Exams");
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        fileMetadata.setParents(Collections.singletonList(examFolderID));

        File studentExamsFolder = driveService.files().create(fileMetadata)
                .setFields("id")
                .execute();
        System.out.println("studentExamsFolder ID: " + studentExamsFolder.getId());

        for (Student s : students) {
            List<Question.QuestionVariant> variant = exam.generateExamVariant();
            System.out.println(s + " : " + variant);
            //buildStudentExamDoc(s, variant, studentExamsFolder.getId(), docsService, driveService);
            buildStudentExamFolder(s, variant, studentExamsFolder.getId(), docsService, driveService);
        }

        return studentExamsFolder.getId();
    }

    public static void shareExamWithStudent(){

    }

    public static void shareExamsWithStudents(List<Student> students, String studentExamsFolderId, Drive driveService) throws IOException {
        JsonBatchCallback<Permission> callback = new JsonBatchCallback<Permission>() {
            @Override
            public void onFailure(GoogleJsonError e,
                                  HttpHeaders responseHeaders)
                    throws IOException {
                // Handle error
                System.err.println(e.getMessage());
            }

            @Override
            public void onSuccess(Permission permission,
                                  HttpHeaders responseHeaders)
                    throws IOException {
                System.out.println("Permission ID: " + permission.getId());
            }
        };

        BatchRequest batch = driveService.batch();

        for (Student s : students) {
            String examId = getStudentExamId(s, studentExamsFolderId, driveService);

            Permission userPermission = new Permission()
                    .setType("user")
                    .setRole("writer")
                    .setEmailAddress(s.getEmail());
            driveService.permissions().create(examId, userPermission)
                    .setFields("id")
                    .queue(batch, callback);
        }

        batch.execute();
    }

    public static void unshareExamsWithStudents(List<Student> students, String studentExamsFolderId, Drive driveService) throws IOException {
        JsonBatchCallback<Void> callback = new JsonBatchCallback<Void>() {
            @Override
            public void onFailure(GoogleJsonError e,
                                  HttpHeaders responseHeaders)
                    throws IOException {
                // Handle error
                System.err.println(e.getMessage());
            }

            @Override
            public void onSuccess(Void v,
                                  HttpHeaders responseHeaders)
                    throws IOException {
            }
        };

        BatchRequest batch = driveService.batch();

        for (Student s : students) {
            String examId = getStudentExamId(s, studentExamsFolderId, driveService);

            List<Permission> currentPermissions = driveService.permissions().list(examId).execute().getPermissions();

            for(Permission p : currentPermissions){
                if(p.getRole().equals("writer"))
                    driveService.permissions().delete(examId, p.getId()).queue(batch, callback);
            }
        }

        batch.execute();
    }


    public static void main(String... args) throws IOException, GeneralSecurityException {
        /* API Setup Stuff */
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Drive driveService = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        Docs docsService = new Docs.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        Sheets sheetsService = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        String folderName = "";

        /* CLI Stuff */
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();

        Option help = Option.builder("h").longOpt("help").desc("print this message.").build();
        Option generate = Option.builder("g").longOpt("generate").desc("generate the exams on Google Drive.").build();
        Option folder = Option.builder("f")
                .longOpt("folder")
                .hasArg(true)
                .argName("name")
                .desc("folder name where the exam is stored on Google Drive. [Required]")
                .build();

        Option share = Option.builder("s")
                .longOpt("share")
                .hasArg(true)
                .optionalArg(true)
                .argName("howlong?")
                .desc("share the exam with all students, for the amount of time (in minutes) specified, or indefinitely if no time given.")
                .build();

        Option unshare = Option.builder("u")
                .longOpt("unshare")
                .desc("unshare the exam with all students.")
                .build();

        options.addOption(help);
        options.addOption(generate);
        options.addOption(folder);
        options.addOption(share);
        options.addOption(unshare);

        try {
            CommandLine line = parser.parse( options, args );

            if (line.hasOption("help") || !line.hasOption("folder")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("Exam Generator", options);
                System.exit(0);
            }

            if (line.hasOption("folder")){
                folderName = line.getOptionValue("folder");
            }

            if (line.hasOption("generate")){
                String examFolderId = getExamFolderId(folderName, driveService);
                List<String> questionFolderIDs = getQuestionFolderIDs(examFolderId, driveService);
                Exam exam = buildExam(questionFolderIDs, driveService);
                String classListId = getClassListId(examFolderId, driveService);
                List<Student> students = getStudents(classListId, sheetsService);
                createStudentExams(examFolderId, exam, students, driveService, docsService);
            }

            if (line.hasOption("share")){
                String howLong = line.getOptionValue("share");

                String examFolderId = getExamFolderId(folderName, driveService);
                String studentExamsFolderId = getStudentExamFolderId(examFolderId, driveService);
                String classListId = getClassListId(examFolderId, driveService);
                List<Student> students = getStudents(classListId, sheetsService);
                shareExamsWithStudents(students, studentExamsFolderId, driveService);

                if (howLong != null){
                    int howlongmins = Integer.parseInt(howLong);

                    try {
                        System.out.printf("Now sharing for %d minutes.\n", howlongmins);
                        Thread.sleep(howlongmins * 60000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    unshareExamsWithStudents(students, studentExamsFolderId, driveService);
                }
            }

            if (line.hasOption("unshare")){
                String examFolderId = getExamFolderId(folderName, driveService);
                String studentExamsFolderId = getStudentExamFolderId(examFolderId, driveService);
                String classListId = getClassListId(examFolderId, driveService);
                List<Student> students = getStudents(classListId, sheetsService);
                unshareExamsWithStudents(students, studentExamsFolderId, driveService);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
