package com.fsoc.template.common.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fsoc.template.R
import com.fsoc.template.common.extension.getTextReplace
import com.fsoc.template.common.extension.isCheck
import com.fsoc.template.common.extension.isCheckTitle
import com.fsoc.template.data.db.entity.ListMessageEntity
import com.fsoc.template.data.db.entity.MessageEntity
import com.fsoc.template.data.db.entity.TypeMessage
import com.fsoc.template.data.db.helper.message.detail.ChatDatabaseHelper
import com.fsoc.template.data.db.helper.message.list.MessagesDatabaseHelper
import com.fsoc.template.domain.entity.PhoneNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.collections.ArrayList

/**
 * Receive Broadcast Receiver.
 */
class ReceiveBroadcastReceiver(
    val phoneNumbers: ArrayList<PhoneNumber>,
    val databaseHelper: MessagesDatabaseHelper,
    val chatDatabaseHelper: ChatDatabaseHelper,
    private val callback: (ListMessageEntity) -> Unit,
    private val callbackMessage: (MessageEntity) -> Unit
) : BroadcastReceiver() {

    @SuppressLint("HardwareIds", "SimpleDateFormat")
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title")
        val text = intent.getStringExtra("text")
        val id = intent.getIntExtra("id", 0)
        val time = intent.getLongExtra("time", 0)
        val type = intent.getIntExtra("type", TypeMessage.TYPE_ZALO.value)
        val isTypeZalo = type == TypeMessage.TYPE_ZALO.value
        val phoneNumber = phoneNumbers.firstOrNull {
            it.name == title
        }?.phoneNumber ?: title ?: ""
        if (text != null) {
            if (isCheck(context, text) && isCheckTitle(context, title ?: "")) {
                main(
                    ListMessageEntity(
                        if (isTypeZalo) id.toString() else phoneNumber,
                        title?.replace(getTextReplace(context, title), "") ?: "",
                        text,
                        false,
                        phoneNumber,
                        type,
                        time
                    )
                )
            }
        }
    }

    fun main(listMessageEntity: ListMessageEntity) = runBlocking<Unit> {
        launch(Dispatchers.IO) {
            val value = databaseHelper.getAllListMessage()
            val message = MessageEntity(
                subId = listMessageEntity.id,
                content = listMessageEntity.lastMessage,
                isUser = false,
                time = listMessageEntity.time
            )
            if (value.any { listMessageEntity.id == it.id }) {
                databaseHelper.insertMessages(listMessageEntity.apply {
                    title = value.firstOrNull { listMessageEntity.id == it.id }?.title
                        ?: ""
                })
            } else {
                databaseHelper.insertMessages(listMessageEntity)
            }
            chatDatabaseHelper.insertMessages(message)
            callbackMessage.invoke(message)
            callback.invoke(listMessageEntity)
        }
    }
}