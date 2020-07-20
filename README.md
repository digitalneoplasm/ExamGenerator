# ExamGenerator
A tool to generate exams using Google Drive. Given variations of questions, this program will create a customized exam for each student. Currently, that means a shared folder with each question in their own Google Doc. (Unfortunately, the API does not yet support appending documents, but I'm looking in to alternative approaches.)

## One-Time Configuration

You will have to do some one-time configuration to get this working. On [this page](https://developers.google.com/drive/api/v3/quickstart/java), click the "Enable Drive API" button, and save the `credentials.json` file to `src/main/java/resources`. Note that if you're in an organization, they may not have enabled this functionality. The first time you run this program it will likely ask to you log into your Google account and verify permissions.

## Setup

To use this software you will have you have your exam configured as follows on Google Drive: 
1) A top-level folder with a unique name for the exam.
2) Inside that folder, folders named Q1, Q2, ... Qn. 
3) Each of the Q folders should contain variations for each question, one per Google Doc. If you want to give the same exam to the whole class you can just put the whole thing in a single Google Doc in any folder starting with a Q.
4) Inside the exam folder a Google Sheet called ClassList with no header row. Column 0 should be a unique identifier for each student, column 1 should be their last name, column 2 their first name, and column 7 their email address.

## Usage

This project uses gradle, so you should just be able to execute `gradle build` followed by `gradle run`, passing arguments to Java using --args

```
usage: Exam Generator
 -f,--folder <name>      folder name where the exam is stored on Google
                         Drive. [Required]
 -g,--generate           generate the exams on Google Drive.
 -h,--help               print this message.
 -s,--share <howlong?>   share the exam with all students, for the amount
                         of time (in minutes) specified, or indefinitely
                         if no time given.
 -u,--unshare            unshare the exam with all students.
```

For example, you might run: 

``` gradle run --args="-f TestExam -g -s 120"```

to generate the TestExam and share it with students for 120 minutes.
## Generating

Generating will produce a folder in your exam folder called `Student Exams`, inside which will be a folder for each student. Those folders are what will be shared with the students, and they will contain a randomly selected variant of each question.


## Sharing and Unsharing

You may share the exam with the class for a given amount of time, or until explicitly unshared. When the share expres students will see a message which says something like "Your access has expired. Reload this document to gain access. If you still don't have access, contact the document owner."