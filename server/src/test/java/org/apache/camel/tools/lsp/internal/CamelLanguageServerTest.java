/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.tools.lsp.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.junit.Test;


public class CamelLanguageServerTest {
	
	private final class DummyLanguageClient implements LanguageClient {
		@Override
		public void telemetryEvent(Object object) {
		}

		@Override
		public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
			return null;
		}

		@Override
		public void showMessage(MessageParams messageParams) {
		}

		@Override
		public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
		}

		@Override
		public void logMessage(MessageParams message) {
		}
	}

	@Test
	public void testProvideDummyCompletionForCamelBlueprintNamespace() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("<from uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(1, 13));
		
		assertThat(completions.get().getLeft()).contains(new CompletionItem("dummyCamelCompletion"));
	}
	
	@Test
	public void testProvideDummyCompletionForCamelSpringNamespace() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("<from uri=\"\" xmlns=\"http://camel.apache.org/schema/spring\"></from>\n");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(1, 11));
		
		assertThat(completions.get().getLeft()).contains(new CompletionItem("dummyCamelCompletion"));
	}

	@Test
	public void testDONTProvideDummyCompletionForNotCamelnamespace() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("<from uri=\"\"></from>\n");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(1, 11));
		
		assertThat(completions.get().getLeft()).isEmpty();
		assertThat(completions.get().getRight()).isNull();
	}

	private CamelLanguageServer initializeLanguageServer(String text) throws URISyntaxException, InterruptedException, ExecutionException {
		InitializeParams params = new InitializeParams();
		params.setProcessId(new Random().nextInt());
		params.setRootUri(getTestResource("/workspace/").toURI().toString());
		CamelLanguageServer camelLanguageServer = new CamelLanguageServer();
		camelLanguageServer.connect(new DummyLanguageClient());
		CompletableFuture<InitializeResult> initialize = camelLanguageServer.initialize(params);
		
		assertThat(initialize).isCompleted();
		assertThat(initialize.get().getCapabilities().getCompletionProvider().getResolveProvider()).isTrue();
		
		camelLanguageServer.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(createTestTextDocument(text)));
		
		return camelLanguageServer;
	}

	private TextDocumentItem createTestTextDocument(String text) {
		return new TextDocumentItem("dummyUri", CamelLanguageServer.LANGUAGE_ID, 0, text);
	}
	
	private CompletableFuture<Either<List<CompletionItem>, CompletionList>> getCompletionFor(CamelLanguageServer camelLanguageServer, Position position) {
		TextDocumentService textDocumentService = camelLanguageServer.getTextDocumentService();
		
		TextDocumentPositionParams dummyCompletionPositionRequest = new TextDocumentPositionParams(new TextDocumentIdentifier("dummyUri"), position);
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = textDocumentService.completion(dummyCompletionPositionRequest);
		return completions;
	}
	
	public File getTestResource(String name) throws URISyntaxException {
		return Paths.get(CamelLanguageServerTest.class.getResource(name).toURI()).toFile();
	}
}
