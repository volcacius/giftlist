package it.polimi.dima.giftlist.presentation.view.fragment;

import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.SwipeDismissItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.decoration.ItemShadowDecorator;
import com.h6ah4i.android.widget.advrecyclerview.decoration.SimpleListDividerDecorator;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;
import com.h6ah4i.android.widget.advrecyclerview.touchguard.RecyclerViewTouchActionGuardManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils;
import com.squareup.picasso.Picasso;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.OnClick;
import hugo.weaving.DebugLog;
import it.polimi.dima.giftlist.R;
import it.polimi.dima.giftlist.data.model.Wishlist;
import it.polimi.dima.giftlist.presentation.component.WishlistListComponent;
import it.polimi.dima.giftlist.presentation.navigation.IntentStarter;
import it.polimi.dima.giftlist.presentation.presenter.WishlistListPresenter;
import it.polimi.dima.giftlist.presentation.view.WishlistListView;
import it.polimi.dima.giftlist.presentation.view.adapter.WishlistListAdapter;

/**
 * Created by Alessandro on 08/01/16.
 */
public class WishlistListFragment extends BaseMvpLceFragment<RecyclerView, List<Wishlist>, WishlistListView, WishlistListPresenter>
        implements WishlistListView {

    @Bind(R.id.contentView)
    RecyclerView recyclerView;
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.collapsing_toolbar)
    CollapsingToolbarLayout collapsingToolbar;
    @Bind(R.id.appbar)
    AppBarLayout appBarLayout;
    @Bind(R.id.backdrop)
    ImageView collapseBackdrop;

    @OnClick(R.id.fab)
    void onFabClick() {
        IntentStarter.startWishlistSettingsActivity(getContext(), Wishlist.DEFAULT_ID, wishlistListAdapter.getWishlistList().size() + 1);
    }

    @Inject
    WishlistListAdapter wishlistListAdapter;
    @Inject
    Picasso picasso;

    RecyclerViewDragDropManager recyclerViewDragDropManager;
    RecyclerViewSwipeManager recyclerViewSwipeManager;
    RecyclerViewTouchActionGuardManager recyclerViewTouchActionGuardManager;
    RecyclerView.Adapter wrappedAdapter;

    private ActionModeCallback actionModeCallback;
    private ActionMode actionMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionModeCallback = new ActionModeCallback();
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_whishlistlist;
    }

    @Override
    protected void injectDependencies() {
        this.getComponent(WishlistListComponent.class).inject(this);
    }

    @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        //to avoid bugs. it would be better to retain selected instances but this is a good enough tradeoff
        if (actionMode!=null) {
            actionMode.finish();
        }
        super.onViewCreated(view, savedInstanceState);

        //Set collapsing bar as activity's actionbar
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(toolbar);
        initCollapsingToolbar();

        wishlistListAdapter.setOnWishlistClickListener(new WishlistListAdapter.OnWishlistClickListener() {
            @Override
            public void onItemClick(View v , int position) {
                if (actionMode != null) {
                    toggleSelection(position);
                } else {
                    IntentStarter.startWishlistActivity(getContext(), wishlistListAdapter.getItemId(position));
                }
            }

            @Override
            public boolean onItemLongClick(View view, int position) {
                if (actionMode == null) {
                    actionMode =((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
                } else {
                    //null
                }
                toggleSelection(position);
                return true;
            }
        });

        // touch guard manager  (this class is required to suppress scrolling while swipe-dismiss animation is running)
        recyclerViewTouchActionGuardManager = new RecyclerViewTouchActionGuardManager();
        recyclerViewTouchActionGuardManager.setInterceptVerticalScrollingWhileAnimationRunning(true);
        recyclerViewTouchActionGuardManager.setEnabled(true);
        // drag & drop manager
        recyclerViewDragDropManager = new RecyclerViewDragDropManager();
        recyclerViewDragDropManager.setDraggingItemShadowDrawable((NinePatchDrawable) ContextCompat.getDrawable(getContext(), R.drawable.material_shadow_z3));
        // swipe manager
        recyclerViewSwipeManager = new RecyclerViewSwipeManager();
        // wrap for dragging
        wrappedAdapter = recyclerViewDragDropManager.createWrappedAdapter(wishlistListAdapter);
        // wrap for swiping
        wrappedAdapter = recyclerViewSwipeManager.createWrappedAdapter(wrappedAdapter);

        GeneralItemAnimator animator = new SwipeDismissItemAnimator();
        // Change animations are enabled by default since support-v7-recyclerview v22.
        // Disable the change animation in order to make turning back animation of swiped item works properly.
        animator.setSupportsChangeAnimations(false);
        recyclerView.setItemAnimator(animator);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        // requires *wrapped* adapter
        recyclerView.setAdapter(wrappedAdapter);
        //For smooth image loading
        recyclerView.setItemViewCacheSize(10);
        recyclerView.setDrawingCacheEnabled(true);

        // additional decorations
        //noinspection StatementWithEmptyBody
        if (supportsViewElevation()) {
            // Lollipop or later has native drop shadow feature. ItemShadowDecorator is not required.
        } else {
            recyclerView.addItemDecoration(new ItemShadowDecorator((NinePatchDrawable) ContextCompat.getDrawable(getContext(), R.drawable.material_shadow_z1)));
        }
        recyclerView.addItemDecoration(new SimpleListDividerDecorator(ContextCompat.getDrawable(getContext(), R.drawable.list_divider_h), true));

        // NOTE:
        // The initialization order is very important! This order determines the priority of touch event handling.
        // priority: TouchActionGuard > Swipe > DragAndDrop
        recyclerViewTouchActionGuardManager.attachRecyclerView(recyclerView);
        recyclerViewSwipeManager.attachRecyclerView(recyclerView);
        recyclerViewDragDropManager.attachRecyclerView(recyclerView);
    }

    @Override
    public void onPause() {
        recyclerViewDragDropManager.cancelDrag();
        //store the order of the wishlists
        presenter.updateWishlistList(wishlistListAdapter.getWishlistList());
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (recyclerViewDragDropManager != null) {
            recyclerViewDragDropManager.release();
            recyclerViewDragDropManager = null;
        }
        if (recyclerViewSwipeManager != null) {
            recyclerViewSwipeManager.release();
            recyclerViewSwipeManager = null;
        }
        if (recyclerViewTouchActionGuardManager != null) {
            recyclerViewTouchActionGuardManager.release();
            recyclerViewTouchActionGuardManager = null;
        }
        if (wrappedAdapter != null) {
            WrapperAdapterUtils.releaseAll(wrappedAdapter);
            wrappedAdapter = null;
        }
        super.onDestroyView();
    }

    private void toggleSelection(int position) {
        wishlistListAdapter.toggleSelection(position);
        int count = wishlistListAdapter.getSelectedItemCount();
        if (count == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(String.valueOf(count));
            actionMode.invalidate();
        }
    }

    @Override protected String getErrorMessage(Throwable e, boolean pullToRefresh) {
        //return errorMessageDeterminer.getErrorMessage(e, pullToRefresh);
        return null;
    }

    @Override public WishlistListPresenter createPresenter() {
        return this.getComponent(WishlistListComponent.class).providePresenter();
    }

    @DebugLog
    @Override
    public void setData(List<Wishlist> data) {
        wishlistListAdapter.setWishlistList(data);
    }

    @Override
    @DebugLog
    public void loadData(boolean pullToRefresh) {
        presenter.subscribe(pullToRefresh);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.menu_wishlistlist, menu);
        final MenuItem searchItem = menu.findItem(R.id.action_add);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                LinkedList<Wishlist> filteredModelList = Wishlist.filter(wishlistListAdapter.getWishlistList(), newText);
                wishlistListAdapter.setFilterableWishlistList(filteredModelList);
                recyclerView.scrollToPosition(0);
                wishlistListAdapter.notifyDataSetChanged();
                return true;
            }
        });
    }

    @Override
    public void removeWishlist(Wishlist wishlist) {
        getPresenter().removeWishlist(wishlist);
    }


    private class ActionModeCallback implements ActionMode.Callback {
        @SuppressWarnings("unused")
        private final String TAG = ActionModeCallback.class.getSimpleName();

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate (R.menu.delete_item_context_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_remove:
                    for (Wishlist w : wishlistListAdapter.getSelectedWishlists()) {
                        removeWishlist(w);
                    }
                    wishlistListAdapter.notifyDataSetChanged();
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            wishlistListAdapter.clearSelection();
            actionMode = null;
        }
    }

    private boolean supportsViewElevation() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
    }

    /**
     * Initializing collapsing toolbar
     * Will show and hide the toolbar title on scroll
     */
    private void initCollapsingToolbar() {
        collapsingToolbar.setTitle(" ");
        collapseBackdrop.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int width = collapseBackdrop.getMeasuredWidth();
        int height = collapseBackdrop.getMeasuredHeight();
        picasso.load(R.drawable.party)
                .resize(width, height)
                .centerCrop()
                .into(collapseBackdrop);

        appBarLayout.setExpanded(true);

        // hiding & showing the title when toolbar expanded & collapsed
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = false;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    collapsingToolbar.setTitle(getString(R.string.app_name));
                    isShow = true;
                } else if (isShow) {
                    collapsingToolbar.setTitle(" ");
                    isShow = false;
                }
            }
        });
    }

    /**
     * RecyclerView item decoration - give equal margin around grid item
    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view); // item position
            int column = position % spanCount; // item column

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
                outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)

                if (position < spanCount) { // top edge
                    outRect.top = spacing;
                }
                outRect.bottom = spacing; // item bottom
            } else {
                outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
                outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
                if (position >= spanCount) {
                    outRect.top = spacing; // item top
                }
            }
        }
    }
     */
}

