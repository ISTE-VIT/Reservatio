package com.vandit.reservatio

import android.content.Context
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
import com.google.firebase.messaging.FirebaseMessaging
import com.vandit.reservatio.databinding.FragmentCameraBinding

private const val CAMERA_REQUEST_CODE = 101

class CameraFragment : Fragment() {
    lateinit var binding: FragmentCameraBinding
    private lateinit var codeScanner: CodeScanner

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

        codeScanner = CodeScanner(requireActivity(), binding.scannerView)
        codeScanner.apply {
            camera = CodeScanner.CAMERA_BACK
            formats = CodeScanner.ALL_FORMATS

            autoFocusMode = AutoFocusMode.SAFE
            scanMode = ScanMode.CONTINUOUS
            isAutoFocusEnabled = true
            isFlashEnabled = false

            decodeCallback = DecodeCallback {
                requireActivity().runOnUiThread {
                    FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            Log.w("TAG", "Fetching FCM registration token failed", task.exception)
                            Toast.makeText(
                                requireContext(),
                                "Fetching FCM token failed",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@OnCompleteListener
                        }

                        // Get new FCM registration token
                        var token = task.result
                        Log.d("CameraFragment.kt", "VANDIT => onViewCreated:65 $token")

                        token = token.replace("""[.:]""".toRegex(), "~_~")
                        Log.d("CameraFragment.kt", "VANDIT => onViewCreated:73 $token")
                        sharedPref.edit().putString("restaurant", it.text).apply()
                        sharedPref.edit().putString("token", token).apply()

                        FirebaseMessaging.getInstance().subscribeToTopic(token)
                            .addOnCompleteListener { task1 ->
                                if (!task1.isSuccessful) {
                                    Log.d("CameraFragment.kt", "VANDIT => onViewCreated:80 => Subscription Failed => ${task1.exception}")
                                } else {
                                    Log.d("CameraFragment.kt", "VANDIT => onViewCreated:80 => Subscription Success")
                                }
                            }
                        fragmentManager?.beginTransaction()
                            ?.replace(R.id.nav_host_fragment, QueueFragment())?.commit()
                    })
                }
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