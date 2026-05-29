package com.example

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: MessageAdapter

    private lateinit var textStatusBarSubtitle: TextView
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var layoutEmptyState: View
    private lateinit var recyclerView: RecyclerView
    
    private lateinit var counterTotal: TextView
    private lateinit var counterPending: TextView
    private lateinit var counterSent: TextView

    private lateinit var textProgressFraction: TextView
    private lateinit var textSuccessRateVal: TextView
    
    private lateinit var btnImport: View
    private lateinit var btnImportEmpty: MaterialButton
    private lateinit var btnStartDispatch: MaterialButton
    private lateinit var btnNextDispatch: MaterialButton

    // Launcher for selecting a CSV file using SAF
    private val selectFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            importFile(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind layouts
        textStatusBarSubtitle = findViewById(R.id.text_status_bar_subtitle)
        loadingSpinner = findViewById(R.id.loading_spinner)
        layoutEmptyState = findViewById(R.id.layout_empty_state)
        recyclerView = findViewById(R.id.recycler_view)
        
        counterTotal = findViewById(R.id.counter_total)
        counterPending = findViewById(R.id.counter_pending)
        counterSent = findViewById(R.id.counter_sent)

        textProgressFraction = findViewById(R.id.text_progress_fraction)
        textSuccessRateVal = findViewById(R.id.text_success_rate_val)
        
        btnImport = findViewById(R.id.btn_import)
        btnImportEmpty = findViewById(R.id.btn_import_empty)
        btnStartDispatch = findViewById(R.id.btn_start_dispatch)
        btnNextDispatch = findViewById(R.id.btn_next_dispatch)

        // Set up list
        adapter = MessageAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Button Click Listeners (both appbar action and landing CTA trigger same selector)
        btnImport.setOnClickListener {
            openFileSelector()
        }
        btnImportEmpty.setOnClickListener {
            openFileSelector()
        }

        btnStartDispatch.setOnClickListener {
            dispatchNextPending()
        }

        btnNextDispatch.setOnClickListener {
            dispatchNextPending()
        }

        // Collect Flow States
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allContacts.collect { contacts ->
                        updateUiState(contacts)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Rule 4 flow control - check if we possess a dispatched ID when returning to UI
        if (viewModel.currentlyDispatchingId != null) {
            viewModel.markCurrentAsSent()
            Toast.makeText(this, "Message marked as sent and updated in list!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFileSelector() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            // Suggest only CSV or plain types if possible
            val mimeTypes = arrayOf("text/comma-separated-values", "text/csv", "application/csv")
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        }
        selectFileLauncher.launch(intent)
    }

    private fun importFile(uri: Uri) {
        loadingSpinner.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        layoutEmptyState.visibility = View.GONE

        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                viewModel.importCsv(inputStream) { count ->
                    loadingSpinner.visibility = View.GONE
                    if (count > 0) {
                        val fileName = getFileNameFromUri(uri)
                        textStatusBarSubtitle.text = "Loaded: $fileName"
                        Toast.makeText(this, "Successfully loaded $count contacts!", Toast.LENGTH_LONG).show()
                    } else {
                        textStatusBarSubtitle.text = "No valid contacts found in file"
                        Toast.makeText(this, "No valid columns matching S.No., Phone, Name, or Message found.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            loadingSpinner.visibility = View.GONE
            Toast.makeText(this, "Failed to read CSV: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "messages_sheet.csv"
    }

    private fun updateUiState(contacts: List<ContactMessage>) {
        if (contacts.isEmpty()) {
            layoutEmptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            btnStartDispatch.isEnabled = false
            btnNextDispatch.isEnabled = false

            textProgressFraction.text = "0 / 0"
            textSuccessRateVal.text = "0%"

            counterTotal.text = "Total: 0"
            counterPending.text = "Pending: 0"
            counterSent.text = "Sent: 0"
            textStatusBarSubtitle.text = "No file loaded"
        } else {
            layoutEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter.submitList(contacts)

            val total = contacts.size
            val pending = contacts.count { it.status.equals("Pending", ignoreCase = true) }
            val sent = total - pending

            counterTotal.text = "Total: $total"
            counterPending.text = "Pending: $pending"
            counterSent.text = "Sent: $sent"

            textProgressFraction.text = "$sent / $total"
            val successRate = if (total > 0) (sent * 100) / total else 0
            textSuccessRateVal.text = "$successRate%"

            btnStartDispatch.isEnabled = pending > 0
            btnNextDispatch.isEnabled = pending > 0
        }
    }

    private fun dispatchNextPending() {
        lifecycleScope.launch {
            val contact = viewModel.getNextPendingContact()
            if (contact != null) {
                dispatchContact(contact)
            } else {
                Toast.makeText(this@MainActivity, "No pending contacts left to dispatch!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun dispatchContact(contact: ContactMessage) {
        try {
            // Register dispatched item id
            viewModel.currentlyDispatchingId = contact.id

            // Clean, escape data and launch Intent
            val encodedMessage = Uri.encode(contact.customMessage)
            val url = "https://api.whatsapp.com/send?phone=${contact.phoneNumber}&text=$encodedMessage"
            
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            viewModel.resetDispatchState()
            Toast.makeText(this, "Failed to launch WhatsApp. Make sure it is installed on this device.", Toast.LENGTH_LONG).show()
        }
    }
}
