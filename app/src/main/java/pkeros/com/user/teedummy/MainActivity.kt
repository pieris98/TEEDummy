package pkeros.com.user.teedummy

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.security.ConfirmationCallback
import android.security.ConfirmationPrompt
import android.util.Log
import android.security.keystore.KeyProperties
import android.security.keystore.KeyGenParameterSpec
import android.support.v7.app.AlertDialog
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.security.*
import java.util.concurrent.Executor
import android.content.DialogInterface
import android.util.Base64
import kotlin.coroutines.experimental.coroutineContext



// This data structure varies by app type. This is just an example.
data class ConfirmationPromptData(val sender: String,
                                  val receiver: String, val amount: String)

//Callback for ConfirmationPrompt
class MyConfirmationCallback : ConfirmationCallback() {
    override fun onConfirmed(dataThatWasConfirmed: ByteArray?) {
        super.onConfirmed(dataThatWasConfirmed)
        // Sign dataThatWasConfirmed using your generated signing key.
        // By completing this process, you generate a "signed statement".
        var keyStore= KeyStore.getInstance("AndroidKeystore")
        keyStore.load(null)
        val privateKeyEntry = keyStore
                ?.getEntry("key1", null) as KeyStore.PrivateKeyEntry

        val privateKey = privateKeyEntry.privateKey

        try {
            val signature = Signature.getInstance("SHA256withRSA/PSS")
            signature.initSign(privateKey)
            signature!!.update(dataThatWasConfirmed)
            val signatureBytes = signature!!.sign()

            // dataThatWasConfirmed and signatureBytes should be sent to RP
            // RP verifies the signature with Kpub and the message (dataThatWasConfirmed) is identical to the challenge
            Log.i("[MainActivity]", "dataThatWasConfirmed: " + Base64.encodeToString(dataThatWasConfirmed, Base64.URL_SAFE))
            Log.i("[MainActivity]", "signature: " + Base64.encodeToString(signatureBytes, Base64.URL_SAFE))
        } catch (e: Exception) {
            throw RuntimeException(e)
        }


    }

    override fun onDismissed() {
        super.onDismissed()
        // Handle case where user declined the prompt in the
        // confirmation dialog.
    }

    override fun onCanceled() {
        super.onCanceled()
        // Handle case where your app closed the dialog before the user
        // could respond to the prompt.
    }

   fun onError(e: Exception?) {
        super.onError(e)
        Log.println(Log.INFO,"onError",e.toString())

        // Handle the exception that the callback captured.
    }
}

class MainActivity : AppCompatActivity() {
    var keyStore= KeyStore.getInstance("AndroidKeystore")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //generating private key
        val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
        keyPairGenerator.initialize(
                KeyGenParameterSpec.Builder(
                        "key1",
                        KeyProperties.PURPOSE_SIGN)
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                        .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PSS).setUserConfirmationRequired(true).setIsStrongBoxBacked(true)
                        .build())

        val keypair=keyPairGenerator.generateKeyPair()


     buttonTest.setOnClickListener() {
         if (!fieldAmount.text.all { it.isDigit() }) {
             Toast.makeText(this, "Amount must be a number!", Toast.LENGTH_LONG).show()
         } else {
             if (fieldName.text.isNullOrEmpty() || fieldAmount.text.isNullOrEmpty()) {
                 Toast.makeText(this, "Please fill in both Name and Amount before pressing the Try button!", Toast.LENGTH_LONG).show()
             } else {
                 //data for ConfirmationPrompt
                 val myExtraData: ByteArray = byteArrayOf()
                 val myDialogData = ConfirmationPromptData("${fieldName.text}", "UoE", "£${fieldAmount.text}")
                 val threadReceivingCallback = Executor { runnable -> runnable.run() }
                 val callback = MyConfirmationCallback()

                 val dialog = ConfirmationPrompt.Builder(this)
                         .setPromptText("${myDialogData.sender}, send ${myDialogData.amount} to ${myDialogData.receiver}?")
                         .setExtraData(myExtraData)
                         .build()
                 dialog.presentPrompt(threadReceivingCallback, callback)
             }
         }
     }
    }
}
