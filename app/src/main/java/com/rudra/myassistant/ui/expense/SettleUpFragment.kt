package com.rudra.myassistant.ui.expense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rudra.myassistant.R
import com.rudra.myassistant.viewmodel.SettleUpViewModel
import kotlinx.coroutines.launch

class SettleUpFragment : Fragment() {

    private val viewModel: SettleUpViewModel by viewModels()
    private var friendUid: String? = null
    private var friendName: String? = null
    private var currentUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            friendUid = it.getString("friendUid")
            friendName = it.getString("friendName")
        }
        currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().uid
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settle_up, container, false)

        view.findViewById<TextView>(R.id.tvSettleLabel).text = "You owe $friendName"
        view.findViewById<View>(R.id.btnCloseSettle).setOnClickListener { parentFragmentManager.popBackStack() }
        
        view.findViewById<View>(R.id.btnConfirmSettleUp).setOnClickListener {
            val uid = currentUid ?: return@setOnClickListener
            val fUid = friendUid ?: return@setOnClickListener
            viewModel.settle(uid, fUid, 0.0)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settlementResult.collect { result ->
                    if (result.isSuccess) {
                        Toast.makeText(context, "Settlement recorded!", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                }
            }
        }

        return view
    }
}
