package com.vandit.reservatio

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.vandit.reservatio.databinding.FragmentQueueBinding

class QueueFragment : Fragment() {
    lateinit var binding: FragmentQueueBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var myRef: DatabaseReference
    private lateinit var sharedPref: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_queue, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ai: ApplicationInfo? = context?.let {
            context?.packageManager
                ?.getApplicationInfo(it.packageName, PackageManager.GET_META_DATA)
        }
        val value = ai?.metaData?.get("FIREBASE_URI")
        val FIREBASE_URI = value.toString()

        sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val name = sharedPref.getString("name", null)
        val un: String? = sharedPref.getString("username", null)
        val restaurant = sharedPref.getString("restaurant", null)
        val token = sharedPref.getString("token", null)

        var userName = "$name-$token"

        database =
            Firebase.database(FIREBASE_URI)
        myRef = database.getReference(restaurant.toString())

        if (un == null) {
            with(sharedPref.edit()) {
                putString("username", userName)
                putBoolean("inQueue", true)
                apply()
            }
            updateDatabase(userName)
            queuePosition(userName)
        } else {
            userName = un
            queuePosition(un)
        }

        binding.leaveBTN.setOnClickListener {

            myRef.get().addOnCompleteListener {
                val resultValue = it.result as DataSnapshot
                val value = resultValue.getValue<HashMap<String, Int>>()
                try {
                    for (x in value?.keys!!) {
                        if (value[x]!! > value[userName]!!) {
                            val temp = value[x]
                            if (temp != null) {
                                value[x] = temp - 1
                            }
                        }

                        if (value[x]!! == value[userName]!!) {
                            value.remove(userName)
                            Log.d("TAG", "value: $value")
                        }
                    }
                } catch (e: Exception) {
                    Log.d("QueueFragment.kt", "VANDIT => onViewCreated:92 => ${e.message}")
                }

                myRef.child(userName).removeValue()
                myRef.setValue(value)

                FirebaseMessaging.getInstance()
                    .unsubscribeFromTopic(token!!)
                    .addOnCompleteListener { task1 ->
                        if (!task1.isSuccessful) {
                            Log.d(
                                "QueueFragment.kt",
                                "VANDIT => onViewCreated:105 => unSubscription failed"
                            )
                        }
                    }
            }

            with(sharedPref.edit()) {
                putString("restaurant", null)
                putString("username", null)
                putBoolean("inQueue", false)
                putInt("flag", 0)
                apply()
            }
            fragmentManager?.beginTransaction()?.replace(R.id.nav_host_fragment, ScanFragment())
                ?.commit()
        }
    }

    private fun updateDatabase(username: String) {
        myRef.get().addOnCompleteListener {
            val resultValue = it.result as DataSnapshot
            val value = resultValue.getValue<HashMap<String, Int>>()
            val size = value?.size

            val map = mapOf(username to size!!)
            myRef.updateChildren(map)
        }
    }

    private fun queuePosition(un: String) {
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue<HashMap<String, Int>>()
                val x = value?.filterKeys { it == un }
                if (x != null) {
                    val flag = sharedPref.getInt("flag", 0)
                    if(flag == 1 && x.values.isEmpty()){
                        with(sharedPref.edit()) {
                            putString("username", null)
                            putBoolean("inQueue", false)
                            putInt("flag", 0)
                            apply()
                        }
                        fragmentManager?.beginTransaction()
                            ?.replace(R.id.nav_host_fragment, FinalFragment())?.commit()
                    }

                    for (i in x.values) {
                        binding.queueCounterTV.text = i.toString()
                        sharedPref.edit().putInt("flag", 1).apply()

                        if (i == 0) {
                            with(sharedPref.edit()) {
                                putString("username", null)
                                putBoolean("inQueue", false)
                                apply()
                            }
                            fragmentManager?.beginTransaction()
                                ?.replace(R.id.nav_host_fragment, FinalFragment())?.commit()
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(activity, "Error Occurred", Toast.LENGTH_SHORT).show()
            }
        })
    }
}