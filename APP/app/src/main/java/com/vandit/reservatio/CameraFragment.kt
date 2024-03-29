package com.vandit.reservatio

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ScanMode
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.vandit.reservatio.databinding.FragmentCameraBinding

private const val CAMERA_REQUEST_CODE = 101

class CameraFragment : Fragment() {
    lateinit var binding: FragmentCameraBinding
    private lateinit var codeScanner: CodeScanner
    private lateinit var database: FirebaseDatabase
    private lateinit var myRef: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_camera, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPermissions()

        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val ai: ApplicationInfo? = context?.let {
            context?.packageManager?.getApplicationInfo(
                it.packageName,
                PackageManager.GET_META_DATA
            )
        }
        val value = ai?.metaData?.get("FIREBASE_URI")
        val FIREBASE_URI = value.toString()
        database = Firebase.database(FIREBASE_URI)

        codeScanner = CodeScanner(requireActivity(), binding.scannerView)
        codeScanner.apply {
            camera = CodeScanner.CAMERA_BACK
            formats = CodeScanner.ALL_FORMATS

            autoFocusMode = AutoFocusMode.SAFE
            scanMode = ScanMode.CONTINUOUS
            isAutoFocusEnabled = true
            isFlashEnabled = false

            decodeCallback = DecodeCallback {
                val prefs =
                    activity?.getSharedPreferences(
                        "TOKEN_PREF",
                        FirebaseMessagingService.MODE_PRIVATE
                    )
                var token = prefs?.getString("token", "")
                token = token?.replace(".", "?")?.replace(":", "%")

                myRef = database.getReference(it.toString())
                myRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val value = snapshot.value
                        if (value != null) {
                            sharedPref.edit().putString("restaurant", it.text).apply()
                            sharedPref.edit().putString("token", token).apply()
                            fragmentManager?.beginTransaction()
                                ?.replace(R.id.nav_host_fragment, QueueFragment())?.commit()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) = Unit
                })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }

    private fun setupPermissions() {
        val permission =
            ContextCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.CAMERA)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(activity, "You need camera permission", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}