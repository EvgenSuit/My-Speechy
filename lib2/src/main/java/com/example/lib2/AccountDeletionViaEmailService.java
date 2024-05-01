package com.example.lib2;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.CacheRequest;
import java.util.Arrays;
import com.example.lib2.SomeKt;
//import com.myspeechy.*;

public class AccountDeletionViaEmailService {

    public static void initFirebase() throws IOException {
        FileInputStream serviceAccount =
                new FileInputStream("C:/Programming/Projects/Kotlin/MySpeechy/lib2/src/main/java/com/example/lib2/my-speechy-firebase-adminsdk-g7v3g-48589351f8.json");
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl("https://my-speechy-default-rtdb.europe-west1.firebasedatabase.app")
                .build();
        FirebaseApp.initializeApp(options);
    }
    private static void deleteUserData() {
        SomeKt.main();
    }
    public static void main(String[] args) throws FirebaseAuthException, IOException {
        initFirebase();
        SomeKt.main();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userEmail = "some@gmail.com";

        /*UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(userEmail);
        auth.createUser(request);*/
        UserRecord userRecord = auth.getUserByEmail(userEmail);
        System.out.println("Successfully fetched user data: "
                + userRecord.getProviderData()[0].getProviderId()
        + ". " + userRecord.getUid());

        //auth.deleteUser(auth.getUserByEmail(userEmail).getUid());
        System.out.println("Successfully deleted user");
    }
}