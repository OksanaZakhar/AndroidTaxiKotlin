package com.ksusha.vel.taxik

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.ksusha.vel.taxik.databinding.ActivityUserSingInBinding


class UserSingInActivity : AppCompatActivity() {

    lateinit var binding: ActivityUserSingInBinding

    lateinit var auth: FirebaseAuth

    private var userIs = ""

    lateinit var sharedPreferences: SharedPreferences
    lateinit var editor: SharedPreferences.Editor


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_sing_in)

        binding = ActivityUserSingInBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)


        binding.userSingInButton.setOnClickListener {
            editor.putString("userIs", ChildDBFirebase.CLIENT.title)
            editor.apply()
            userIs = ChildDBFirebase.CLIENT.title
            loginUser()
        }

        binding.driverSingInButton.setOnClickListener {
            editor.putString("userIs", ChildDBFirebase.DRIVER.title)
            editor.apply()
            userIs = ChildDBFirebase.DRIVER.title
            loginUser()
        }

    }


    override fun onStart() {
        super.onStart()
        sharedPreferences = getSharedPreferences(
            "sharedPreferences",
            MODE_PRIVATE
        )
        editor = sharedPreferences.edit()
        userIs = sharedPreferences.getString("userIs", "").toString()
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            if (userIs == ChildDBFirebase.CLIENT.title) {
                startActivity(
                    Intent(
                        this,
                        ClientMapsActivity::class.java
                    )
                )
            } else {
                if (userIs == ChildDBFirebase.DRIVER.title) startActivity(
                    Intent(
                        this,
                        DriverMapsActivity::class.java
                    )
                )
            }
        }
    }

    private fun validateEmail(): Boolean {

        val emailInput = binding.textInputEmail.editText!!.text
            .trim()
        return if (emailInput.isEmpty()) {
            binding.textInputEmail.error = "Please input your email"
            false
        } else {
            binding.textInputEmail.error = ""
            true
        }
    }

    private fun validateName(): Boolean {
        val nameInput = binding.textInputName.editText!!.text.toString()
            .trim()
        return if (nameInput.isEmpty()) {
            binding.textInputName.error = "Please input your name"
            false
        } else if (nameInput.length > 15) {
            binding.textInputName.error = "Name length have to be less than 15"
            false
        } else {
            binding.textInputName.error = ""
            true
        }
    }

    private fun validatePassword(): Boolean {
        val passwordInput = binding.textInputPassword.editText!!.text
            .toString().trim()
        return if (passwordInput.isEmpty()) {
            binding.textInputPassword.error = "Please input your password"
            false
        } else if (passwordInput.length < 7) {
            binding.textInputPassword.error = "Password length have to be more than 6"
            false
        } else {
            binding.textInputPassword.error = ""
            true
        }
    }

    private fun validateConfirmPassword(): Boolean {
        val passwordInput = binding.textInputPassword.editText!!.text
            .toString().trim()
        val confirmPasswordInput = binding.textInputConfirmPassword.editText!!.text
            .toString().trim()
        return if (passwordInput != confirmPasswordInput) {
            binding.textInputPassword.error = "Passwords have to match"
            false
        } else {
            binding.textInputPassword.error = ""
            true
        }
    }

    private fun loginUser() {
        if (!validateEmail() or !validateName() or !validatePassword() or
            !validateConfirmPassword()
        ) {
            return
        }
        auth.createUserWithEmailAndPassword(
            binding.textInputEmail.editText!!.text.toString().trim(),
            binding.textInputPassword.editText!!.text.toString().trim()
        )
            .addOnCompleteListener(
                this
            ) { task ->
                if (task.isSuccessful) {
                    Log.d("TAG", "createUserWithEmail:success")
                    val user = auth.currentUser
                    if (userIs == ChildDBFirebase.CLIENT.title) startActivity(
                        Intent(
                            this@UserSingInActivity,
                            ClientMapsActivity::class.java
                        )
                    )
                    if (userIs == ChildDBFirebase.DRIVER.title) startActivity(
                        Intent(
                            this@UserSingInActivity,
                            DriverMapsActivity::class.java
                        )
                    )
                } else {
                    Log.w(
                        "TAG", "createUserWithEmail:failure",
                        task.exception
                    )
                    Toast.makeText(
                        this@UserSingInActivity,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }


}