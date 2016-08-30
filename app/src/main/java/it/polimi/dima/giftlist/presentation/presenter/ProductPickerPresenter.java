package it.polimi.dima.giftlist.presentation.presenter;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Environment;
import android.provider.SyncStateContract;

import com.pushtorefresh.storio.contentresolver.operations.put.PutResults;
import com.pushtorefresh.storio.sqlite.StorIOSQLite;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import hugo.weaving.DebugLog;
import it.polimi.dima.giftlist.data.model.EbayProduct;
import it.polimi.dima.giftlist.data.model.EtsyProduct;
import it.polimi.dima.giftlist.data.model.Product;
import it.polimi.dima.giftlist.presentation.event.AdapterAboutToEmptyEvent;
import it.polimi.dima.giftlist.presentation.event.ProductAddedEvent;
import it.polimi.dima.giftlist.presentation.event.ProductImageSavedEvent;
import it.polimi.dima.giftlist.presentation.exception.UnknownProductException;
import it.polimi.dima.giftlist.presentation.view.ProductPickerView;
import it.polimi.dima.giftlist.domain.interactor.GetNetProductsUseCase;
import it.polimi.dima.giftlist.util.ImageConstants;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

/**
 * Created by Elena on 27/01/2016.
 */
public class ProductPickerPresenter extends BaseRxLcePresenter<ProductPickerView, List<Product>, GetNetProductsUseCase> {

    private static final boolean NO_PULL_TO_REFRESH = false;

    private boolean isSubscriptionPending;
    Picasso picasso;

    //needed to avoid that the GC collects the image before it is stored
    final List<Target> targets;

    @Inject
    public ProductPickerPresenter(EventBus eventBus, GetNetProductsUseCase getNetProductsUseCase, StorIOSQLite db, Picasso picasso) {
        super(eventBus, getNetProductsUseCase, db);
        this.picasso = picasso;
        this.isSubscriptionPending = false;
        this.targets = new ArrayList<Target>();
    }

    /**
     * Subscribes the presenter himself as subscriber on the observable
     * This pattern of calling onCompleted, onError and onNext of the Presenter from the inner subscriber class
     * allows, if necessary, to override them from classes that extends the Presenter while leaving the subscriber untouched.
     * To get an idea, see e.g. https://ideone.com/mIeavZ
     *
     * ShowLoading is not called here, otherwise it would show the loading view when subscribing in background, even if there
     * is still stuff in the adapter. ShowLoading is called only as an empty view of the adapter
     *
     * @param pullToRefresh Pull to refresh?
     */
    @Override
    @DebugLog
    public void subscribe(boolean pullToRefresh) {
        useCase.execute(new BaseSubscriber(pullToRefresh));
    }

    @Override
    protected void onCompleted() {
        if (isViewAttached()) {
            getView().showContent();
        }
        unsubscribe();
        checkPendingSubscription();
    }

    @Override
    protected void onError(Throwable e, boolean pullToRefresh) {
        if (isViewAttached()) {
            getView().showError(e, pullToRefresh);
        }
        unsubscribe();
        checkPendingSubscription();
    }

    @Override
    protected void onNext(List<Product> data) {
        if (isViewAttached()) {
            getView().appendData(data);
            getView().showContent();
        }
    }

    //This is way to manage messages back from the view layer. When the adapter is emptying,
    //it sends a message to the presenter to remind it to load more data. If a subscription isn't ongoing,
    //subscribe. Otherwise, if a subscription is already open and there is no pending future subscription,
    //set a subscription as pending.
    @DebugLog
    @Subscribe
    public void onAdapterEmptyEvent(AdapterAboutToEmptyEvent event) {
        if (useCase.isUnsubscribed()) {
            subscribe(NO_PULL_TO_REFRESH);
        } else if (!isSubscriptionPending) {
            isSubscriptionPending = true;
        }
    }



    @Subscribe
    public void onProductAddedEvent(ProductAddedEvent event) throws UnknownProductException {
        Product product = event.getProduct();
        saveProductImage(product);


    }

    @Subscribe
    public void onProductImageSaved(ProductImageSavedEvent event) throws UnknownProductException {

        Observer observer;
        Product product = event.getProduct();
        if (product instanceof EbayProduct) {
            observer = new EbayProductPutObserver();
        } else if (product instanceof EtsyProduct) {
            observer = new EtsyProductPutObserver();
        } else {
            throw new UnknownProductException();
        }

        db.put()
                .object(product)
                .prepare()
                .asRxObservable()
                .observeOn(AndroidSchedulers.mainThread()) //all Observables in StorIO already subscribed on Schedulers.io(), you just need to set observeOn()
                .subscribe(observer);
    }

    //Check if there is a pending subscription to register
    private void checkPendingSubscription() {
        if (isSubscriptionPending) {
            subscribe(NO_PULL_TO_REFRESH);
            isSubscriptionPending = false;
        }
    }

    private class EbayProductPutObserver implements Observer<PutResults<EbayProduct>> {
        @Override
        public void onCompleted() {
        }
        @Override
        public void onError(Throwable e) {
            getView().showProductAddedError();
        }
        @Override
        public void onNext(PutResults<EbayProduct> ebayProductPutResults) {
            getView().showProductAddedSuccess();
        }
    }

    private class EtsyProductPutObserver implements Observer<PutResults<EtsyProduct>> {
        @Override
        public void onCompleted() {
        }
        @Override
        public void onError(Throwable e) {
            getView().showProductAddedError();
        }
        @Override
        public void onNext(PutResults<EtsyProduct> etsyProductPutResults) {
            getView().showProductAddedSuccess();
        }
    }


    public void saveProductImage(Product product) throws UnknownProductException {

        Target myTarget = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                try {
                    Timber.d("picasso is saving pic: " + product.getName());
                    File myDir = new File(product.getImageUri());
                    FileOutputStream out = new FileOutputStream(myDir);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    out.flush();
                    out.close();
                    eventBus.post(new ProductImageSavedEvent(product));
                } catch(Exception e){
                    Timber.d("picasso error " + e.getMessage());
                }
                targets.remove(this);
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                Timber.d("picasso onBitmapFailed");
                //Better to remove uri?
                eventBus.post(new ProductImageSavedEvent(product));
                targets.remove(this);
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                Timber.d("picasso onPrepareLoad");
            }
        };

        targets.add(myTarget);
        picasso
                .load(product.getImageUrl())
                .resize(ImageConstants.IMAGE_WIDTH_SAVE, ImageConstants.IMAGE_HEIGHT_SAVE)//The other dimension will be scaled properly preserving aspect ratio
                .centerInside()
                .onlyScaleDown()
                .into(myTarget);

    }


}


