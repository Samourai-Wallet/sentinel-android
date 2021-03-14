package com.samourai.sentinel.ui.collectionDetails

import androidx.lifecycle.*
import com.samourai.sentinel.data.PubKeyCollection
import com.samourai.sentinel.data.repository.CollectionRepository
import com.samourai.sentinel.data.repository.ExchangeRateRepository
import com.samourai.sentinel.ui.utils.PrefsUtil
import com.samourai.sentinel.util.MonetaryUtil
import org.koin.java.KoinJavaComponent.inject

/**
 * sentinel-android
 *
 * @author Sarath
 */
class CollectionDetailsViewModel(private val pubKeyCollection: PubKeyCollection) : ViewModel() {

    private val collectionRepository: CollectionRepository by inject(CollectionRepository::class.java)
    private val prefsUtil: PrefsUtil by inject(PrefsUtil::class.java)
    private val exchangeRateRepository: ExchangeRateRepository by inject(ExchangeRateRepository::class.java)


    fun getRepository(): CollectionRepository {
        return collectionRepository
    }

    fun getCollections(): LiveData<ArrayList<PubKeyCollection>> {
        return collectionRepository.collectionsLiveData
    }

    fun getBalance(): LiveData<Long> {
        val mediator = MediatorLiveData<Long>();
        mediator.addSource(collectionRepository.collectionsLiveData, Observer {
            mediator.value = it.findLast { collection -> collection.balance == pubKeyCollection.balance }?.balance
        })
        return mediator

    }

    fun getFiatBalance(): LiveData<String> {
        val mediator = MediatorLiveData<String>();
        mediator.addSource(collectionRepository.collectionsLiveData, Observer {
            val balance = it.findLast { collection -> collection.balance == pubKeyCollection.balance }?.balance
            mediator.value = getFiatBalance(balance, exchangeRateRepository.getRateLive().value)
        })
        mediator.addSource(exchangeRateRepository.getRateLive(), Observer {
            val balance = collectionRepository.collectionsLiveData.value?.findLast { collection -> collection.balance == pubKeyCollection.balance }?.balance
            mediator.value = getFiatBalance(balance, it)
        })
        return mediator

    }


    private fun getFiatBalance(balance: Long?, rate: ExchangeRateRepository.Rate?): String {
        if (rate != null) {
            balance?.let {
                return try {
                    val fiatRate = MonetaryUtil.getInstance().getFiatFormat(prefsUtil.selectedCurrency)
                            .format((balance / 1e8) * rate.rate)
                    "$fiatRate ${rate.currency}"
                } catch (e: Exception) {
                    "00.00 ${rate.currency}"
                }
            }
            return "00.00"
        } else {
            return "00.00"
        }
    }

    class CollectionDetailsViewModelFactory(private val pubKeyCollection: PubKeyCollection) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CollectionDetailsViewModel::class.java)) {
                return CollectionDetailsViewModel(pubKeyCollection) as T
            }
            throw IllegalArgumentException("Unknown ViewMo del class")
        }
    }

    companion object {
        fun getFactory(pubKeyCollection: PubKeyCollection): CollectionDetailsViewModelFactory {
            return CollectionDetailsViewModelFactory(pubKeyCollection)
        }
    }
}