package com.partyplanner.presentation.event

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.partyplanner.data.local.SessionStorage
import com.partyplanner.domain.usecase.event.DeleteEventUseCase
import com.partyplanner.domain.usecase.event.GetEventUseCase
import com.partyplanner.domain.usecase.invitation.GetEventInvitationsUseCase
import com.partyplanner.domain.usecase.invitation.GetInviteSuggestionsUseCase
import com.partyplanner.domain.usecase.invitation.InviteByEmailUseCase
import com.partyplanner.domain.usecase.invitation.InviteByUserIdUseCase
import com.partyplanner.domain.usecase.invitation.RsvpToInvitationUseCase
import com.partyplanner.domain.model.InvitationStatus
import com.partyplanner.domain.repository.ChatRepository
import com.partyplanner.domain.usecase.carpool.CreateCarpoolOfferUseCase
import com.partyplanner.domain.usecase.carpool.DeleteCarpoolOfferUseCase
import com.partyplanner.domain.usecase.carpool.GetCarpoolOffersUseCase
import com.partyplanner.domain.usecase.carpool.JoinCarpoolUseCase
import com.partyplanner.domain.usecase.carpool.LeaveCarpoolUseCase
import com.partyplanner.domain.usecase.carpool.UpdateCarpoolOfferUseCase
import com.partyplanner.domain.usecase.item.AddItemBroughtUseCase
import com.partyplanner.domain.usecase.item.AddItemRequestUseCase
import com.partyplanner.domain.usecase.item.DeleteItemBroughtUseCase
import com.partyplanner.domain.usecase.item.DeleteItemRequestUseCase
import com.partyplanner.domain.usecase.item.FulfillItemRequestUseCase
import com.partyplanner.domain.usecase.item.GetCategoriesUseCase
import com.partyplanner.domain.usecase.item.GetItemsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DefaultEventDetailComponent(
    componentContext: ComponentContext,
    private val eventId: Int,
    private val getEventUseCase: GetEventUseCase,
    private val deleteEventUseCase: DeleteEventUseCase,
    private val onBack: () -> Unit,
    private val onEdit: () -> Unit = {},
) : EventDetailComponent, ComponentContext by componentContext, KoinComponent {

    private val sessionStorage: SessionStorage                         by inject()
    private val getEventInvitationsUseCase: GetEventInvitationsUseCase by inject()
    private val inviteByEmailUseCase: InviteByEmailUseCase               by inject()
    private val inviteByUserIdUseCase: InviteByUserIdUseCase             by inject()
    private val getInviteSuggestionsUseCase: GetInviteSuggestionsUseCase by inject()
    private val rsvpToInvitationUseCase: RsvpToInvitationUseCase        by inject()
    private val getCategoriesUseCase: GetCategoriesUseCase  by inject()
    private val getItemsUseCase: GetItemsUseCase            by inject()
    private val addItemRequestUseCase: AddItemRequestUseCase by inject()
    private val fulfillItemRequestUseCase: FulfillItemRequestUseCase by inject()
    private val deleteItemRequestUseCase: DeleteItemRequestUseCase by inject()
    private val addItemBroughtUseCase: AddItemBroughtUseCase by inject()
    private val deleteItemBroughtUseCase: DeleteItemBroughtUseCase by inject()
    private val getCarpoolOffersUseCase: GetCarpoolOffersUseCase by inject()
    private val createCarpoolOfferUseCase: CreateCarpoolOfferUseCase by inject()
    private val updateCarpoolOfferUseCase: UpdateCarpoolOfferUseCase by inject()
    private val deleteCarpoolOfferUseCase: DeleteCarpoolOfferUseCase by inject()
    private val joinCarpoolUseCase: JoinCarpoolUseCase by inject()
    private val leaveCarpoolUseCase: LeaveCarpoolUseCase by inject()
    private val chatRepository: ChatRepository by inject()

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob()).also {
        lifecycle.doOnDestroy(it::cancel)
    }

    private val _state = MutableStateFlow<EventDetailState>(EventDetailState.Loading)
    override val state: StateFlow<EventDetailState> = _state.asStateFlow()

    init {
        scope.launch {
            val currentUserId = sessionStorage.getSession()?.userId?.toInt() ?: 0
            getEventUseCase(eventId).fold(
                onSuccess = { event ->
                    val isOwner = event.ownerId == currentUserId
                    _state.value = EventDetailState.Success(
                        event = event,
                        isOwner = isOwner,
                        currentUserId = currentUserId,
                        currentUserInvitationStatus = event.currentUserInvitationStatus,
                    )
                    loadInvitations()
                    loadItems()
                    loadCategories()
                    loadCarpoolOffers()
                    connectChat()
                    if (isOwner) loadInviteSuggestions()
                },
                onFailure = { _state.value = EventDetailState.Error(it.message ?: "Erreur inconnue") }
            )
        }
    }

    private fun loadInviteSuggestions() {
        scope.launch {
            getInviteSuggestionsUseCase(eventId).onSuccess { suggestions ->
                updateSuccess { copy(inviteSuggestions = suggestions) }
            }
        }
    }

    private fun loadInvitations() {
        scope.launch {
            getEventInvitationsUseCase(eventId).onSuccess { invitations ->
                updateSuccess { copy(invitations = invitations) }
            }
        }
    }

    private fun loadCategories() {
        scope.launch {
            getCategoriesUseCase().onSuccess { cats ->
                updateSuccess { copy(categories = cats) }
            }
        }
    }

    private fun loadItems() {
        scope.launch {
            getItemsUseCase(eventId).onSuccess { items ->
                updateSuccess { copy(items = items) }
            }
        }
    }

    private var isChatTabActive = false

    override fun onChatRead() {
        isChatTabActive = true
        updateSuccess { copy(unreadChatCount = 0) }
    }

    override fun onChatLeft() {
        isChatTabActive = false
    }

    private fun connectChat() {
        scope.launch {
            chatRepository.messages.collect { message ->
                updateSuccess {
                    copy(
                        chatMessages    = chatMessages + message,
                        unreadChatCount = if (!isChatTabActive) unreadChatCount + 1 else 0,
                    )
                }
            }
        }
        scope.launch {
            var backoff = 2_000L
            while (true) {
                runCatching { chatRepository.connect(eventId) }
                    .onFailure { println("Chat WS error: $it") }
                delay(backoff)
                backoff = (backoff * 2).coerceAtMost(30_000L)
            }
        }
    }

    private fun loadCarpoolOffers() {
        scope.launch {
            getCarpoolOffersUseCase(eventId).onSuccess { offers ->
                updateSuccess { copy(carpoolOffers = offers) }
            }
        }
    }

    override fun onBack() = onBack.invoke()
    override fun onEdit() = onEdit.invoke()

    override fun onDelete() {
        scope.launch {
            deleteEventUseCase(eventId)
                .onSuccess { onBack.invoke() }
                .onFailure { e -> updateSuccess { copy(deleteError = e.message ?: "Erreur lors de la suppression") } }
        }
    }

    override fun onAddItemRequest(label: String, quantity: Int, categoryId: Int?) {
        scope.launch {
            addItemRequestUseCase(eventId, label, quantity, categoryId).onSuccess { newRequest ->
                updateSuccess { copy(items = items.copy(requests = items.requests + newRequest)) }
            }
        }
    }

    override fun onFulfillItemRequest(requestId: Int) {
        scope.launch {
            fulfillItemRequestUseCase(eventId, requestId).onSuccess { updated ->
                updateSuccess {
                    copy(items = items.copy(requests = items.requests.map {
                        if (it.id == requestId) updated else it
                    }))
                }
            }
        }
    }

    override fun onDeleteItemRequest(requestId: Int) {
        scope.launch {
            deleteItemRequestUseCase(eventId, requestId).onSuccess {
                updateSuccess { copy(items = items.copy(requests = items.requests.filter { it.id != requestId })) }
            }
        }
    }

    override fun onAddItemBrought(label: String, quantity: Int, categoryId: Int?) {
        scope.launch {
            addItemBroughtUseCase(eventId, label, quantity, categoryId).onSuccess {
                loadItems()
            }
        }
    }

    override fun onDeleteItemBrought(broughtId: Int) {
        scope.launch {
            deleteItemBroughtUseCase(eventId, broughtId).onSuccess {
                updateSuccess { copy(items = items.copy(brought = items.brought.filter { it.id != broughtId })) }
            }
        }
    }

    override fun onCreateCarpoolOffer(seats: Int, departurePoint: String?, notes: String?) {
        scope.launch {
            createCarpoolOfferUseCase(eventId, seats, departurePoint, notes).onSuccess { offer ->
                updateSuccess { copy(carpoolOffers = carpoolOffers + offer) }
            }
        }
    }

    override fun onUpdateCarpoolOffer(offerId: Int, seats: Int, departurePoint: String?, notes: String?) {
        scope.launch {
            updateCarpoolOfferUseCase(eventId, offerId, seats, departurePoint, notes).onSuccess { updated ->
                updateSuccess { copy(carpoolOffers = carpoolOffers.map { if (it.id == offerId) updated else it }) }
            }
        }
    }

    override fun onDeleteCarpoolOffer(offerId: Int) {
        scope.launch {
            deleteCarpoolOfferUseCase(eventId, offerId).onSuccess {
                updateSuccess { copy(carpoolOffers = carpoolOffers.filter { it.id != offerId }) }
            }
        }
    }

    override fun onJoinCarpool(offerId: Int, pickupPoint: String?) {
        scope.launch {
            joinCarpoolUseCase(eventId, offerId, pickupPoint).onSuccess { updated ->
                updateSuccess { copy(carpoolOffers = carpoolOffers.map { if (it.id == offerId) updated else it }) }
            }
        }
    }

    override fun onSendMessage(content: String) {
        scope.launch { chatRepository.send(content) }
    }

    override fun onInviteByUserId(userId: Int) {
        scope.launch {
            inviteByUserIdUseCase(eventId, userId)
                .onSuccess { invitation ->
                    updateSuccess {
                        copy(
                            invitations      = invitations + invitation,
                            inviteSuggestions = inviteSuggestions.filter { it.id != userId },
                            inviteEmailResult = InviteEmailResult.Success(invitation.userDisplayName),
                        )
                    }
                }
                .onFailure { e ->
                    updateSuccess { copy(inviteEmailResult = InviteEmailResult.Error(e.message ?: "Erreur")) }
                }
        }
    }

    override fun onInviteByEmail(email: String) {
        scope.launch {
            inviteByEmailUseCase(eventId, email)
                .onSuccess { invitation ->
                    updateSuccess {
                        copy(
                            invitations       = invitations + invitation,
                            inviteSuggestions = inviteSuggestions.filter { it.id != invitation.userId },
                            inviteEmailResult = InviteEmailResult.Success(invitation.userDisplayName),
                        )
                    }
                }
                .onFailure { e ->
                    updateSuccess { copy(inviteEmailResult = InviteEmailResult.Error(e.message ?: "Erreur")) }
                }
        }
    }

    override fun onDismissInviteResult() {
        updateSuccess { copy(inviteEmailResult = null) }
    }

    override fun onDismissDeleteError() {
        updateSuccess { copy(deleteError = null) }
    }

    override fun onRsvp(status: InvitationStatus) {
        val token = (state.value as? EventDetailState.Success)?.event?.inviteToken ?: return
        scope.launch {
            rsvpToInvitationUseCase(token, status).onSuccess {
                updateSuccess { copy(currentUserInvitationStatus = status) }
            }
        }
    }

    override fun onRefresh() {
        updateSuccess { copy(isRefreshing = true) }
        scope.launch {
            val inv     = async { getEventInvitationsUseCase(eventId) }
            val items   = async { getItemsUseCase(eventId) }
            val carpool = async { getCarpoolOffersUseCase(eventId) }
            inv.await().onSuccess     { invitations -> updateSuccess { copy(invitations = invitations) } }
            items.await().onSuccess   { i           -> updateSuccess { copy(items = i) } }
            carpool.await().onSuccess { offers      -> updateSuccess { copy(carpoolOffers = offers) } }
            updateSuccess { copy(isRefreshing = false) }
        }
    }

    override fun onLeaveCarpool(offerId: Int) {
        scope.launch {
            leaveCarpoolUseCase(eventId, offerId).onSuccess { updated ->
                updateSuccess { copy(carpoolOffers = carpoolOffers.map { if (it.id == offerId) updated else it }) }
            }
        }
    }

    private inline fun updateSuccess(block: EventDetailState.Success.() -> EventDetailState.Success) {
        val current = _state.value as? EventDetailState.Success ?: return
        _state.value = current.block()
    }
}
