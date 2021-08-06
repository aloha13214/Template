package com.fsoc.template.common.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import com.fsoc.template.R
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

    private fun isCheck(context: Context, text: String): Boolean {
        val lst = context.resources.getStringArray(R.array.message).toCollection(arrayListOf())
        var isCheck = true
        lst.forEach {
            if (text.contains(it, true)) {
                isCheck = false
                return@forEach
            }
        }
        return isCheck
    }

    private fun isCheckTitle(context: Context, text: String): Boolean {
        val lst = context.resources.getStringArray(R.array.titleMessage).toCollection(arrayListOf())
        var isCheck = true
        lst.forEach {
            if (text.contains(it, true)) {
                isCheck = false
                return@forEach
            }
        }
        return isCheck
    }

    private fun getTextReplace(context: Context, title: String?): String {
        val tmp = context.resources.getStringArray(R.array.tin_nhan).toCollection(arrayListOf())
        var replace = ""
        tmp.forEach {
            if (title?.contains(it) == true) {
                replace = it
            }
        }
        return replace
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