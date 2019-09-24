package com.vucko.cometchatdemo

import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.pro.constants.CometChatConstants
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.core.MessagesRequest
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.*

class GroupActivity : AppCompatActivity() {

    private val stopTypingRunnable: Runnable = Runnable { endTyping() }
    // Used to determine after how much time user will be declared as no longer typing (in milliseconds)
    private val TYPING_DELAY: Long = 5000
    private val TYPING_THRESHOLD_TEXT = 2
    lateinit var messagesRecyclerView: RecyclerView
    lateinit var messageEditText: EditText
    lateinit var noMessagesGroup: androidx.constraintlayout.widget.Group
    lateinit var typingLayout: LinearLayout
    lateinit var typingTextView: TextView
    val currentlyTyping: MutableList<User?> = ArrayList()
    var isTyping = false

    val typingHandler = Handler()
    var group: Group? = null

    lateinit var messagesAdapter: MessagesAdapter


    // Some random ID for the listener for now
    private val listenerID: String = "1234"
    private val typingListenerId: String = "TypingListener 1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        typingLayout = findViewById(R.id.typingLayout)
        typingTextView = findViewById(R.id.typingTextView)

        getGroupDetailsAndMessages()
        messagesAdapter = MessagesAdapter(ArrayList(), this)
        messagesRecyclerView.adapter = messagesAdapter
        messagesRecyclerView.layoutManager = LinearLayoutManager(this)
        noMessagesGroup = findViewById(R.id.noMessagesGroup)
        messageEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                attemptSendMessage()
                true
            } else {
                false
            }
        }
        messageEditText.setOnKeyListener { _, _, _ ->
            startTypingTimer()
            true
        }
    }

    private fun setTypingListener() {
        CometChat.addMessageListener(typingListenerId, object : CometChat.MessageListener() {
            override fun onTypingEnded(typingIndicator: TypingIndicator?) {
                removeTypingUser(typingIndicator?.sender)
            }

            override fun onTypingStarted(typingIndicator: TypingIndicator?) {
                addTypingUser(typingIndicator?.sender)
            }

        })
    }

    private fun addTypingUser(user: User?) {
        for (u in currentlyTyping) {
            if (u?.uid == user?.uid) {
                return
            }
        }
        currentlyTyping.add(user)
        notifyTypingChanged()
    }

    private fun removeTypingUser(user: User?) {
        for (u in currentlyTyping) {
            if (u?.uid == user?.uid) {
                currentlyTyping.remove(u)
            }
        }
        notifyTypingChanged()
    }

    private fun notifyTypingChanged() {
        if (currentlyTyping.size == 0) {
            typingLayout.visibility = View.INVISIBLE
        } else {

            messagesRecyclerView.smoothScrollToPosition(messagesAdapter.itemCount - 1)
            typingLayout.visibility = View.VISIBLE
            formatTypingMessage(currentlyTyping)
        }
    }

    private fun formatTypingMessage(currentlyTyping: MutableList<User?>) {
        if (currentlyTyping.size >= TYPING_THRESHOLD_TEXT) {
            typingTextView.text = "Several people are typing"
        } else {
            if (currentlyTyping.size == 1) {
                typingTextView.text = currentlyTyping[0]?.name + " is typing"
            } else {
                typingTextView.text = currentlyTyping[0]?.name + " and " + currentlyTyping[1]?.name + " are typing"
            }
        }
    }

    private fun startTypingTimer() {
        if (!isTyping) {
            startTyping()
        }
        isTyping = true
        typingHandler.removeCallbacks(stopTypingRunnable)
        typingHandler.postDelayed(stopTypingRunnable, TYPING_DELAY)
    }

    private fun startTyping() {
        val typingIndicator = TypingIndicator(group!!.guid, CometChatConstants.RECEIVER_TYPE_GROUP)
        CometChat.startTyping(typingIndicator)
    }

    private fun endTyping() {
        val typingIndicator = TypingIndicator(group!!.guid, CometChatConstants.RECEIVER_TYPE_GROUP)
        CometChat.endTyping(typingIndicator)
        isTyping = false
        typingHandler.removeCallbacks(stopTypingRunnable)
    }

    private fun getGroupDetailsAndMessages() {
        // Get the details of the group, such as name, members and other data that may be used in the app later
        // For now only the group name is used

        val groupId = intent.getStringExtra("group_id")
        CometChat.getGroup(groupId, object : CometChat.CallbackListener<Group>() {
            override fun onSuccess(p0: Group?) {
                group = p0
                updateUI()
            }

            override fun onError(p0: CometChatException?) {

            }
        })
        val messagesRequest = MessagesRequest.MessagesRequestBuilder().setGUID(groupId).build()
        messagesRequest.fetchPrevious(object : CometChat.CallbackListener<List<BaseMessage>>() {
            override fun onSuccess(p0: List<BaseMessage>?) {
                if (!p0.isNullOrEmpty()) {
                    for (baseMessage in p0) {
                        if (baseMessage is TextMessage) {
                            addMessage(baseMessage)
                        }
                    }
                }
            }

            override fun onError(p0: CometChatException?) {
                Toast.makeText(this@GroupActivity, p0?.message, Toast.LENGTH_SHORT).show()
            }
        })

    }

    private fun updateUI() {
        supportActionBar?.title = group?.name
    }

    private fun attemptSendMessage() {
        // Attempts to send the message to the group by current user
        val text = messageEditText.text.toString()
        if (!TextUtils.isEmpty(text)) {
            messageEditText.text.clear()
            val receiverID: String = group!!.guid
            val messageType: String = CometChatConstants.MESSAGE_TYPE_TEXT
            val receiverType: String = CometChatConstants.RECEIVER_TYPE_GROUP

            val textMessage = TextMessage(receiverID, text, messageType, receiverType)

            CometChat.sendMessage(textMessage, object : CometChat.CallbackListener<TextMessage>() {
                override fun onSuccess(p0: TextMessage?) {
                    addMessage(p0)
                    endTyping()
                }

                override fun onError(p0: CometChatException?) {

                }

            })
        }

    }

    private fun addMessage(message: TextMessage?) {
        noMessagesGroup.visibility = View.GONE
        messagesAdapter.addMessage(message)
        messagesRecyclerView.smoothScrollToPosition(messagesAdapter.itemCount - 1)
    }

    override fun onResume() {
        super.onResume()
        setTypingListener()
        // Add the listener to listen for incoming messages in this screen
        setIncomingMessageListener()
    }

    private fun setIncomingMessageListener() {
        CometChat.addMessageListener(listenerID, object : CometChat.MessageListener() {

            override fun onTextMessageReceived(message: TextMessage?) {
                addMessage(message)
            }

            override fun onMediaMessageReceived(message: MediaMessage?) {

            }

        })
    }

    override fun onPause() {
        super.onPause()
        CometChat.removeMessageListener(listenerID)
        CometChat.removeMessageListener(typingListenerId)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // Respond to the action bar's Up/Home button
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
