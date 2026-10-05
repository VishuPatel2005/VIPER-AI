package com.rudra.myassistant.ui.expense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rudra.myassistant.R
import com.rudra.myassistant.adapter.ParticipantSplitAdapter
import com.rudra.myassistant.viewmodel.AddExpenseViewModel
import kotlinx.coroutines.launch

class AddExpenseFragment : Fragment() {

    private val viewModel: AddExpenseViewModel by viewModels()
    private lateinit var etAmount: EditText
    private lateinit var etTitle: EditText
    private lateinit var adapter: ParticipantSplitAdapter
    private lateinit var tvEmptyState: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_add_expense, container, false)

        etAmount = view.findViewById(R.id.etExpenseAmount)
        etTitle = view.findViewById(R.id.etExpenseTitle)
        tvEmptyState = view.findViewById(R.id.tvFriendsEmptyState)
        
        val rvParticipants = view.findViewById<RecyclerView>(R.id.rvAddParticipants)
        rvParticipants.layoutManager = LinearLayoutManager(context)
        
        adapter = ParticipantSplitAdapter(
            isSelected = { uid -> viewModel.isSelected(uid) },
            onFriendClicked = { friend -> 
                viewModel.toggleParticipant(friend)
                // Adapter uses ListAdapter with DiffUtil, but selection state is external, 
                // so we might need to trigger a refresh if the list content itself doesn't change.
                // However, ListAdapter + StateFlow usually handles this if we submit a new list.
                // To be safe, we refresh the specific item or just submitList again.
                adapter.notifyDataSetChanged() 
            }
        )
        rvParticipants.adapter = adapter

        view.findViewById<View>(R.id.btnCloseAddExpense).setOnClickListener { parentFragmentManager.popBackStack() }
        view.findViewById<View>(R.id.btnSubmitExpense).setOnClickListener {
            val title = etTitle.text.toString().trim()
            val amountStr = etAmount.text.toString().trim()
            if (title.isNotEmpty() && amountStr.isNotEmpty()) {
                viewModel.saveExpense(title, amountStr.toDouble())
            } else {
                Toast.makeText(context, "Please enter title and amount", Toast.LENGTH_SHORT).show()
            }
        }

        observeViewModel()

        return view
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.friends.collect { friendList ->
                        adapter.submitList(friendList)
                        tvEmptyState.isVisible = friendList.isEmpty()
                    }
                }
                
                launch {
                    viewModel.saveResult.collect { result ->
                        if (result.isSuccess) {
                            Toast.makeText(context, "Expense saved!", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        } else {
                            Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                launch {
                    viewModel.errorEvent.collect { error ->
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
