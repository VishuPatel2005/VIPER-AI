package com.rudra.myassistant.helper;

import androidx.lifecycle.LifecycleOwner;
import com.rudra.myassistant.viewmodel.HomeViewModel;
import com.rudra.myassistant.viewmodel.HomeSuggestionState;

public class HomeSuggestionObserver {
    
    public interface HomeSuggestionListener {
        void onSuggestionUpdated(HomeSuggestionState state);
    }

    private final LifecycleOwner lifecycleOwner;
    private final HomeViewModel viewModel;
    private final HomeSuggestionListener listener;

    public HomeSuggestionObserver(LifecycleOwner lifecycleOwner, HomeViewModel viewModel, HomeSuggestionListener listener) {
        this.lifecycleOwner = lifecycleOwner;
        this.viewModel = viewModel;
        this.listener = listener;
    }

    public void start() {
        androidx.lifecycle.LifecycleOwnerKt.getLifecycleScope(lifecycleOwner).launchWhenStarted(new kotlin.jvm.functions.Function2<kotlinx.coroutines.CoroutineScope, kotlin.coroutines.Continuation<? super kotlin.Unit>, Object>() {
            @Override
            public Object invoke(kotlinx.coroutines.CoroutineScope scope, kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
                return viewModel.getSuggestionState().collect(new kotlinx.coroutines.flow.FlowCollector<HomeSuggestionState>() {
                    @Override
                    public Object emit(HomeSuggestionState state, kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
                        listener.onSuggestionUpdated(state);
                        return kotlin.Unit.INSTANCE;
                    }
                }, continuation);
            }
        });
    }
}
