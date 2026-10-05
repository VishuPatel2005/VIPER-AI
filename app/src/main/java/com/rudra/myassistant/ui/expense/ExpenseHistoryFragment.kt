package com.rudra.myassistant.ui.expense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rudra.myassistant.R

class ExpenseHistoryFragment : Fragment() {

    private var friendUid: String? = null
    private var friendName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            friendUid = it.getString("friendUid")
            friendName = it.getString("friendName")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_expense_history, container, false)

        view.findViewById<TextView>(R.id.tvHistoryFriendName).text = friendName
        view.findViewById<View>(R.id.btnBackHistory).setOnClickListener { parentFragmentManager.popBackStack() }
        
        view.findViewById<View>(R.id.btnSettleUpLedger).setOnClickListener {
            val settle = SettleUpFragment().apply {
                arguments = Bundle().apply {
                    putString("friendUid", friendUid)
                    putString("friendName", friendName)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerConnect, settle)
                .addToBackStack(null)
                .commit()
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvLedgerItems)
        recyclerView.layoutManager = LinearLayoutManager(context)

        return view
    }
}
