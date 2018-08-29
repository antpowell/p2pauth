package com.anthony_powell.p2pauth

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.android.synthetic.main.activity_main.*
import com.google.android.gms.nearby.connection.Strategy.P2P_STAR
import android.support.annotation.NonNull
import android.support.v4.app.NotificationCompat
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.nearby.connection.AdvertisingOptions
import android.support.v4.util.SimpleArrayMap




class MainActivity : AppCompatActivity() {
    private val TAG = "P2P Communication"

    private val REQUIRED_PERMISSIONS = arrayOf(

            Manifest.permission.BLUETOOTH,

            Manifest.permission.BLUETOOTH_ADMIN,

            Manifest.permission.ACCESS_WIFI_STATE,

            Manifest.permission.CHANGE_WIFI_STATE,

            Manifest.permission.ACCESS_COARSE_LOCATION)

    private val incomingPayloads = SimpleArrayMap<Long, NotificationCompat.Builder>()
    private val outgoingPayloads = SimpleArrayMap<Long, NotificationCompat.Builder>()

    private val codeName: String = CodeNameGenerator.generate()

    private var connectionsClient: ConnectionsClient? = null

    private var instructorEndpointId: String = ""
    private var instructorName: String = ""

    private val REQUEST_CODE_REQUIRED_PERMISSIONS = 1

    private val STRATEGY = Strategy.P2P_CLUSTER

    private val payloadCallback = object : PayloadCallback() {


        override fun onPayloadReceived(endpointId: String, payload: Payload) {

            main_text_view.text = "found a payload at $endpointId"

        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {

            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {

                main_text_view.text = "found a payload"

            }

        }

    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(p0: String, p1: DiscoveredEndpointInfo) {
            Log.i(TAG, "endpoint found, connecting")
            main_text_view.text = "endpoint found, connecting to $p1"

            connectionsClient?.requestConnection(instructorName, p0, connectionLifecycleCallback)
                    ?.addOnSuccessListener {
                        Log.i(TAG, "connection request accepted")
                        main_text_view.text = "connection request accepted"
                    }
                    ?.addOnFailureListener {
                        Log.i(TAG, "connection request denied")
                        main_text_view.text = "connection request denied"
                    }

        }

        override fun onEndpointLost(p0: String) {
            Log.i(TAG, "endpoint lost")
            main_text_view.text = "endpoint lost"
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(p0: String, p1: ConnectionInfo) {
            Log.i(TAG, "initiating connection")
            main_text_view.text = "initiating connection"
            connectionsClient?.acceptConnection(p0, payloadCallback)
            instructorName = p1.endpointName
        }

        override fun onConnectionResult(p0: String, p1: ConnectionResolution) {
            if (p1.status.isSuccess) {
                main_text_view.text = "onConnectionResults: connection successful"
                Log.i(TAG, "onConnectionResults: connection successful")

                connectionsClient?.stopDiscovery()
                connectionsClient?.stopAdvertising()

                instructorEndpointId = p0

            } else {
                main_text_view.text = "onConnectionResults: connection failed"
                Log.i(TAG, "onConnectionResults: connection failed")
            }
        }

        override fun onDisconnected(p0: String) {
            main_text_view.text = "onDisconnected: disconnected from instructor"
            Log.i(TAG, "onDisconnected: disconnected from instructor")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS)
        }


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            main_text_view.text = "Sign over your life... \nPlz =)"
            Toast.makeText(this, "Sign over your life... plz =)", Toast.LENGTH_SHORT).show()
        } else {
            main_text_view.text = "Thanks for your life!"
            Toast.makeText(this, "Thanks for your life", Toast.LENGTH_SHORT).show()
        }

        connectionsClient = Nearby.getConnectionsClient(this)
        listeners()

//        Nearby.getConnectionsClient(this)
//                .startAdvertising(
//                        /* endpointName= */ "Device A",
//                        /* serviceId= */ "com.example.package_name",
//                        mConnectionLifecycleCallback,
//                        new AdvertisingOptions(Strategy.P2P_CLUSTER))

//        main_text_view.text = "Try This!"

    }

    override fun onStart() {
        super.onStart()
        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS)
        }
    }

    override fun onStop() {
        connectionsClient?.stopAllEndpoints()
        super.onStop()
    }

    private fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        for (permission in permissions) {
            Log.i(TAG, "Checking permission $permission")
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "permission $permission: not granted")
                return false
            }
            Log.i(TAG, "permission $permission: granted")
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != REQUEST_CODE_REQUIRED_PERMISSIONS) {
            return
        }

        for (grantResult in grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {

                Toast.makeText(this, "needed permission missing", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        }
        recreate()
    }

    private fun findInstructor() {
        startAdvertising()
        startDiscovery()
    }

    private fun disconnected() {
        main_text_view.text = "disconnecting"
        connectionsClient?.disconnectFromEndpoint(instructorEndpointId)
        connectionsClient?.stopAdvertising()
        connectionsClient?.stopDiscovery()
        main_text_view.text = "disconnected"
    }

    private fun startDiscovery() {
        connectionsClient
                ?.startDiscovery(packageName, endpointDiscoveryCallback, DiscoveryOptions(STRATEGY))
                ?.addOnSuccessListener {
                    main_text_view.text = "We're discovering"
                    Log.i(TAG, "We're discovering")
                }
                ?.addOnFailureListener {
                    main_text_view.text = "Unable to start discovering"
                    Log.i(TAG, "Unable to start discovering")
                }
    }

    private fun startAdvertising() {
        connectionsClient
                ?.startAdvertising(codeName, packageName, connectionLifecycleCallback, AdvertisingOptions(STRATEGY))
                ?.addOnSuccessListener {
                    // We're advertising!
                    Log.i(TAG, "We're advertising")
                    main_text_view.text = "We're advertising"
                }
                ?.addOnFailureListener { e ->
                    // We were unable to start advertising.
                    Log.i(TAG, "Advertising failure: $e")
                    main_text_view.text = "Advertising failure: $e"
                }
    }

    private fun listeners() {
        activate_button.setOnClickListener {
            Log.i(TAG, "Activate findInstructor")
            main_text_view.text = "Activate findInstructor"
            findInstructor()
        }
        deactivate_button.setOnClickListener {
            Log.i(TAG, "Deactivate and disconnect")
            main_text_view.text = "Deactivate and disconnect"
            disconnected()
        }
        advertise_button.setOnClickListener {
            Log.i(TAG, "Advertising...")
            main_text_view.text = "Advertising..."
            startAdvertising()
        }
        detect_button.setOnClickListener {
            Log.i(TAG, "Detecting")
            main_text_view.text = "Detecting"
            startDiscovery()
        }
        send_message_button
                .setOnClickListener {
                    Log.i(TAG, "sending password: ${password_text_input.text}")
                    main_text_view.text = "sending password: ${password_text_input.text}"
                }
    }

    private fun sendPayload(endpointId: String, payload: Payload){
        if(payload.type == Payload.Type.BYTES) return
    }


}
