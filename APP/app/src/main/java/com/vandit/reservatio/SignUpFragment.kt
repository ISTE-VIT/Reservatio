package com.vandit.reservatio

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.vandit.reservatio.databinding.FragmentSignUpBinding


class SignUpFragment : Fragment() {
    lateinit var binding: FragmentSignUpBinding

    // Declare the launcher at the top of your Activity/Fragment:
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("SignUpFragment.kt", "VANDIT => onViewCreated:60 => Success")
        } else {
            Log.d("SignUpFragment.kt", "VANDIT => onViewCreated:63 => Denied")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_sign_up, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val flag = sharedPref.getString("name", null)
        if (flag != null) {
            fragmentManager?.beginTransaction()?.replace(R.id.nav_host_fragment, ScanFragment())
                ?.commit()
        }

        binding.apply {
            submitBTN.setOnClickListener {
                if (nameET.text.toString().trim() != "") {
                    with(sharedPref.edit()) {
                        putString("name", nameET.text.toString())
                        apply()
                    }

                    fragmentManager?.beginTransaction()
                        ?.replace(R.id.nav_host_fragment, ScanFragment())?.commit()
                } else {
                    errorTV.visibility = View.VISIBLE
                }
            }
        }
    }
}