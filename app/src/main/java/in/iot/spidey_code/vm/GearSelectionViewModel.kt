package `in`.iot.spidey_code.vm

import androidx.lifecycle.ViewModel
import `in`.iot.spidey_code.data.model.FilterType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GearSelectionViewModel : ViewModel() {

    private val _selectedFilter = MutableStateFlow(FilterType.CLASSIC_MASK)
    val selectedFilter: StateFlow<FilterType> = _selectedFilter.asStateFlow()

    fun selectFilter(filter: FilterType) {
        _selectedFilter.value = filter
    }
}
