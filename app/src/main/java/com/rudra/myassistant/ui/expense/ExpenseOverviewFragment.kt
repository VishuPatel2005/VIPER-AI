package com.rudra.myassistant.ui.expense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.rudra.myassistant.R
import com.rudra.myassistant.adapter.ExpenseListAdapter
import com.rudra.myassistant.viewmodel.SharedExpensesViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

class ExpenseOverviewFragment : Fragment() {

    private val viewModel: SharedExpensesViewModel by viewModels()
    private lateinit var adapter: ExpenseListAdapter
    private lateinit var tvNetBalance: TextView
    private lateinit var chipOwe: TextView
    private lateinit var chipOwed: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_expense_overview, container, false)
        
        tvNetBalance = view.findViewById(R.id.tvNetBalanceLarge)
        chipOwe = view.findViewById(R.id.chipOweSummary)
        chipOwed = view.findViewById(R.id.chipOwedSummary)
        
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvFriendBalances)
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        adapter = ExpenseListAdapter { summary ->
            val history = ExpenseHistoryFragment().apply {
                arguments = Bundle().apply {
                    putString("friendUid", summary.friend.uid)
                    putString("friendName", summary.friend.displayName)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerConnect, history)
                .addToBackStack(null)
                .commit()
        }
        recyclerView.adapter = adapter

        view.findViewById<View>(R.id.btnBackOverview).setOnClickListener { parentFragmentManager.popBackStack() }
        view.findViewById<View>(R.id.btnAddExpenseOverview).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerConnect, AddExpenseFragment())
                .addToBackStack(null)
                .commit()
        }

        FirebaseAuth.getInstance().uid?.let { viewModel.setCurrentUid(it) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.balances.collect { balances ->
                    adapter.setBalances(balances)
                    
                    var totalOwe = 0.0
                    var totalOwed = 0.0
                    balances.forEach { 
                        if (it.direction == com.rudra.myassistant.model.BalanceDirection.YOU_OWE) totalOwe += it.amount
                        else if (it.direction == com.rudra.myassistant.model.BalanceDirection.THEY_OWE) totalOwed += it.amount
                    }
                    
                    val net = totalOwed - totalOwe
                    tvNetBalance.text = "₹${net.toInt()}"
                    chipOwe.text = "You owe ₹${totalOwe.toInt()}"
                    chipOwed.text = "You are owed ₹${totalOwed.toInt()}"
                }
            }
        }

        return view
    }
}
