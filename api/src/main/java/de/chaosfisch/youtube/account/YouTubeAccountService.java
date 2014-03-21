/**************************************************************************************************
 * Copyright (c) 2014 Dennis Fischer.                                                             *
 * All rights reserved. This program and the accompanying materials                               *
 * are made available under the terms of the GNU Public License v3.0+                             *
 * which accompanies this distribution, and is available at                                       *
 * http://www.gnu.org/licenses/gpl.html                                                           *
 *                                                                                                *
 * Contributors: Dennis Fischer                                                                   *
 **************************************************************************************************/

package de.chaosfisch.youtube.account;

import de.chaosfisch.data.account.AccountDTO;
import de.chaosfisch.data.account.AccountType;
import de.chaosfisch.data.account.IAccountDAO;
import de.chaosfisch.data.account.fields.FieldDTO;
import de.chaosfisch.youtube.YouTubeFactory;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class YouTubeAccountService implements IAccountService {
	private final SimpleListProperty<AccountModel> accountModels = new SimpleListProperty<>(
			FXCollections.observableArrayList());
	@NotNull
	private final IAccountDAO accountDAO;

	@Inject
	public YouTubeAccountService(@NotNull final IAccountDAO accountDAO) {
		this.accountDAO = accountDAO;
		loadAccounts();
	}

	private void loadAccounts() {
		accountModels.addAll(getAll());
		Collections.sort(accountModels);
	}

	@Override
	public SimpleListProperty<AccountModel> accountModelsProperty() {
		return accountModels;
	}

	@Override
	public void remove(final AccountModel accountModel) {
		accountModels.remove(accountModel);
		accountDAO.remove(toDTO(accountModel));
	}

	@Override
	public String getRefreshToken(final String authorizationCode) throws IOException {
		return YouTubeFactory.getRefreshToken(authorizationCode);
	}

	@Override
	public void store(final AccountModel accountModel) {
		accountModels.add(accountModel);
		accountDAO.store(toDTO(accountModel));
	}

	private AccountDTO toDTO(final AccountModel accountModel) {
		return new AccountDTO(accountModel.getYoutubeId(),
							  accountModel.getName(),
							  accountModel.getEmail(),
							  accountModel.getRefreshToken(),
							  accountModel.getType()
										  .name(),
							  accountModel.getFields()
										  .stream()
										  .map(fieldName -> new FieldDTO(accountModel.getYoutubeId(), fieldName))
										  .collect(Collectors.toList()),
							  new ArrayList<>(accountModel.getCookies())
		);
	}

	private AccountModel fromDTO(final AccountDTO accountDTO) {
		final AccountModel accountModel = new AccountModel();
		accountModel.setYoutubeId(accountDTO.getYoutubeId());
		accountModel.setName(accountDTO.getName());
		accountModel.setEmail(accountDTO.getEmail());
		accountModel.setRefreshToken(accountDTO.getRefreshToken());
		accountModel.setType(AccountType.valueOf(accountDTO.getType()));
		accountModel.setFields(FXCollections.observableArrayList(accountDTO.getFields()
																		   .stream()
																		   .map(FieldDTO::getName)
																		   .collect(Collectors.toList())));
		accountModel.setCookies(FXCollections.observableSet(accountDTO.getCookies()
																	  .stream()
																	  .collect(Collectors.toSet())));

		return accountModel;
	}

	public void setAccountModels(final ObservableList<AccountModel> accountModels) {
		this.accountModels.set(accountModels);
	}

	public List<AccountModel> getAll() {
		return accountDAO.getAll()
						 .stream()
						 .map(this::fromDTO)
						 .collect(Collectors.toList());
	}
}