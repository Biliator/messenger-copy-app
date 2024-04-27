package com.example.kotlinmessanger

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_register.*
import java.util.*

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        register_button.setOnClickListener {
            performRegister()
        }

        have_already_account.setOnClickListener {
            Log.d("RegisterActivity", "Try to show login activity")

            // launch the login activity somehow
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // vybrat obrazek z knihovny
        add_picture.setOnClickListener {
            Intent(Intent.ACTION_PICK).also { intent ->
                intent.type = "image/*" //vybere obrazek
                startActivityForResult(intent, 0)
            }
        }
    }

    var selectedPhotoUri: Uri? = null

    // tohle se vykona po intentu
    // vlastne to vyhledava uloziste obrazku
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            Log.d("RegisterActivity", "Photo was selected")

            // vezme obrazek z mobuilu a da ho na profil
            selectedPhotoUri = data.data

            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUri) // typ bitmap

            //val bitmapDrawable = BitmapDrawable(bitmap) // prevede na typ Drawable
            //add_picture.setBackgroundDrawable(bitmapDrawable) // prevede na typ Button
            profile_photo.setImageBitmap(bitmap)
            add_picture.alpha = 0f // schova button za obrazek
        }
    }

    // vykona register
    private fun performRegister() {
        val email = email_edittext.text.toString()
        val password = password_edittext.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("RegisterActivity", "Email is: $email")
        Log.d("RegisterActivity", "Password is: $password")

        // Firebase
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful) return@addOnCompleteListener //neni li uspesna registrace vyvola se znovu

                // else if successful
                Log.d("RegisterActivity", "Successfuly created user with uid: ${it.result!!.user.uid}")// ukaze id uzyvatele

                uploadImageToFirebaseStorage()
            }
            .addOnFailureListener {
                Log.d("RegisterActivity", "Failed to create user: ${it.message}")
                Toast.makeText(this, "Failed to create user: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ulozeni obrazku na firebase
    private fun uploadImageToFirebaseStorage() {
        if (selectedPhotoUri == null) return // kontroluje zda obrazek vubec existuje

        // vytvori nahodne id pro obrazek
        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename") //ulozi na Firebasu do slozky images/

        // pridani obrazku do firebase
        ref.putFile(selectedPhotoUri!!)
            .addOnSuccessListener {
                Log.d("RegisterActivity", "Successfully uploaded image: ${it.metadata?.path}") //zobrazi cestu k obrazku na Firebesu

                ref.downloadUrl.addOnSuccessListener { //downloadUrl -- adresa obrazku
                    Log.d("RegisterActivity", "File Locatio: $it")

                    //vytvoreni uzivatele na databazi ve firebasu
                    saveUserToFirebaseDatabase(it.toString())
                }
            }
            .addOnFailureListener {
                Log.d("RegisterActivity", "ERROR: ${it.message}")
            }

    }

    private fun saveUserToFirebaseDatabase(profileImageUrl: String) {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

        // vytvori podslozky usera ve Firebasu
        val user = User(uid, username_edittext.text.toString(), profileImageUrl)
        ref.setValue(user).addOnSuccessListener {
            Log.d("RegisterActivity", "Finally we saved the user to Firebase Database")

            // po registraci se presune do LatestMessangerActivity
            val intent = Intent(this, LatestMessageActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK) // kdybych dal sipku spet, tak me to nehodi na register, ale na plochu
            startActivity(intent)
        }
            .addOnFailureListener {
                Log.d("RegisterActivity", "ERROR:  ${it.message}")
            }
    }
}

