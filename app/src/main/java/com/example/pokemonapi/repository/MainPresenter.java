package com.example.pokemonapi.repository;

import com.example.pokemonapi.database.DatabaseBuilder;
import com.example.pokemonapi.database.PokemonListDAO;
import com.example.pokemonapi.model.pokemonlist.PokemonListAPI;
import com.example.pokemonapi.model.pokemonlist.ResultsResponse;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainPresenter implements Contracts.MainPresenter {

    private final Contracts.MainView mainView;
    private final Contracts.Model model;
    private final PokemonListDAO pokemonListDAO;
    private List<ResultsResponse> data;
    private Disposable disposable;

    public MainPresenter(Contracts.MainView mainView, Contracts.Model model) {
        this.mainView = mainView;
        this.model = model;
        pokemonListDAO = DatabaseBuilder.getINSTANCE().databaseBuilder().pokemonListDAO();
    }

    @Override
    public void fetchPokemonList(int offset) {
        //pokemonListDAO.deleteAll(); // use to clear old database
        mainView.showProgressBar();
        data = new ArrayList<>();

        Observable<PokemonListAPI> pokemonListAPIObservable = model.observableFetchPokemonList(offset);
        Observer<PokemonListAPI> pokemonListAPIObserver = getPokemonListAPIObserver(offset);

        pokemonListAPIObservable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pokemonListAPIObserver);
    }

    private Observer<PokemonListAPI> getPokemonListAPIObserver(int offset) {
        return new Observer<PokemonListAPI>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                disposable = d;
            }

            @Override
            public void onNext(@NonNull PokemonListAPI pokemonListAPI) {
                onResponseSuccess(pokemonListAPI, offset);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                onResponseFail(e, offset);
            }

            @Override
            public void onComplete() {

            }
        };
    }

    private void onResponseSuccess(PokemonListAPI pokemonListAPI, int offset) {
        List<ResultsResponse> resultsList = pokemonListAPI.getResults();

        for (int i = 0; i < resultsList.size(); i++) {
            ResultsResponse result = resultsList.get(i);

            String namePoke = result.getName();

            String urlPoke = result.getUrl().replaceFirst(".$", "").substring(33);

            data.add(new ResultsResponse(offset, namePoke, urlPoke));
        }

        mainView.hideProgressBar();
        mainView.onOnlineResponse(data);
        pokemonListDAO.insertPokemonList(data);
    }

    private void onResponseFail(Throwable e, int offset) {
        mainView.hideProgressBar();
        mainView.onFailure(e.toString());

        if (pokemonListDAO.getPokemonList(offset) != null) {
            mainView.onOfflineResponse(pokemonListDAO.getPokemonList(offset));
            mainView.toastForOfflineMode();
        }
    }

    public void getDisposableToUnsubscribe() {
        disposable.dispose();
    }
}
