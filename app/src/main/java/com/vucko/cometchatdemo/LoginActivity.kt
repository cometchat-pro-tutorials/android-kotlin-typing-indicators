package com.vucko.cometchatdemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.cometchat.pro.core.AppSettings
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.Group
import com.cometchat.pro.models.User

class LoginActivity : AppCompatActivity() {

    lateinit var usernameEditText: EditText
    lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        findViewById<ImageButton>(R.id.backArrowImageButton).setOnClickListener { onBackPressed() }
        loginButton = findViewById(R.id.logInButton)
        loginButton.setOnClickListener { attemptLogin() }
        usernameEditText = findViewById(R.id.usernameEditText)
        usernameEditText.setOnEditorActionListener {
                _, actionId, _ ->
            if(actionId == EditorInfo.IME_ACTION_DONE){
                attemptLogin()
                true
            } else {
                false
            }
        }
    }

    private fun attemptLogin() {
        loggingInButtonState()
        val UID = usernameEditText.text.toString()


        CometChat.login(UID, GeneralConstants.API_KEY, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User?) {
                redirectToSuperGroup()
            }

            override fun onError(p0: CometChatException?) {
                normalButtonState()
                Toast.makeText(this@LoginActivity, p0?.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loggingInButtonState() {
        loginButton.text = getString(R.string.logging_in)
        loginButton.isEnabled = false
    }

    private fun normalButtonState() {
        loginButton.text = getString(R.string.log_in)
        loginButton.isEnabled = true
    }

    private fun redirectToSuperGroup() {
        getSuperGroupDetails()
    }

    private fun getSuperGroupDetails() {
        // We want to automatically join the already existing group with guid 'supergroup'
        val GUID:String="supergroup"

        CometChat.getGroup(GUID,object :CometChat.CallbackListener<Group>(){
            override fun onSuccess(p0: Group?) {
                goToGroupScreen(p0!!)
                normalButtonState()
            }
            override fun onError(p0: CometChatException?) {

            }
        })
    }

    private fun goToGroupScreen(group: Group) {
        val intent = Intent(this, GroupActivity::class.java)
        intent.putExtra("group_id", group.guid)
        startActivity(intent)
    }
}

