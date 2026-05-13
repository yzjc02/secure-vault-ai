(function () {
    "use strict";

    const tokenKey = "secureVaultToken";
    const usernameKey = "secureVaultUsername";
    const languageKey = "secureVaultLanguage";
    const defaultLanguage = "zh-CN";
    const supportedLanguages = ["zh-CN", "en-US"];

    const I18N = {
        "zh-CN": {
            documentTitle: "Secure Vault AI",
            appTitle: "Secure Vault AI",
            appSubtitle: "隐私优先的个人知识库",
            languageLabel: "语言",
            languageSelectorLabel: "选择界面语言",
            knowledgeSidebarAria: "知识库侧边栏",
            detailsPanelAria: "详情面板",
            authenticationAria: "认证",
            status: "状态",
            alreadyLoggedIn: "已登录",
            alreadyLoggedInWithUser: "已登录：{username}",
            pleaseSignIn: "请先登录",
            register: "注册",
            login: "登录",
            logout: "退出登录",
            username: "用户名",
            email: "邮箱",
            password: "密码",
            usernamePlaceholder: "请输入用户名",
            emailPlaceholder: "请输入邮箱",
            passwordPlaceholder: "请输入密码",
            createAccount: "创建账号",
            signIn: "登录",
            newChat: "新会话",
            knowledgeFiles: "知识库文件",
            uploadFile: "上传文件",
            refresh: "刷新",
            refreshConversations: "刷新会话",
            auditLogs: "审计日志",
            conversations: "会话",
            documents: "文档",
            noDocumentsYet: "暂无文档",
            uploadKnowledgeFile: "上传知识库文件",
            selectFile: "选择文件",
            file: "文件",
            title: "标题",
            description: "描述",
            optionalTitle: "可选标题",
            upload: "上传",
            uploadSuccess: "上传成功",
            pleaseSelectFile: "请选择文件",
            localRagWorkspace: "本地 RAG 工作区",
            knowledgeChat: "知识库问答",
            chatSubtitle: "基于你上传的知识库文件提问。",
            welcomeMessage: "上传文件，点击向量化，然后开始提问。每个回答下方会显示来源。",
            questionPlaceholder: "请输入你的问题……",
            questionAria: "问题",
            ask: "提问",
            sending: "正在发送……",
            answer: "回答",
            you: "你",
            sources: "来源",
            noSourcesReturned: "未返回来源。",
            noReadablePreview: "该来源未返回可读片段。",
            source: "来源",
            sourceNumber: "来源 {number}",
            document: "文档",
            documentId: "文档 ID",
            position: "位置",
            chunkPositionValue: "分片 {index}",
            match: "匹配度",
            preview: "片段预览",
            scoreUnavailable: "匹配度不可用",
            unknown: "未知",
            action: "操作",
            resource: "资源",
            resourceId: "资源 ID",
            success: "成功",
            failed: "失败",
            message: "消息",
            createdAt: "创建时间",
            noAuditLogsYet: "暂无审计日志",
            embed: "向量化",
            delete: "删除",
            view: "查看",
            confirmDelete: "确认删除？",
            confirmDeleteWithName: "确认删除 {name}？",
            deleteSuccess: "删除成功",
            embedSuccess: "向量化成功",
            operationSucceeded: "操作成功",
            operationFailed: "操作失败",
            unauthorized: "未授权，请重新登录。",
            networkError: "网络错误，请确认后端服务正在运行。",
            loading: "加载中……",
            copied: "已复制",
            requestFailed: "请求失败。",
            httpRequestFailed: "请求失败，HTTP 状态码 {status}。",
            creatingAccount: "正在创建账号……",
            registrationComplete: "注册完成，现在可以登录。",
            signingIn: "正在登录……",
            signedInStatus: "已登录。",
            signedOutStatus: "已退出登录。",
            signedOutMessage: "你已退出登录。请登录后使用你的私有知识库。",
            usernameEmailPasswordRequired: "用户名、邮箱和密码为必填项。",
            usernamePasswordRequired: "用户名和密码为必填项。",
            loginMissingToken: "登录响应没有包含 token。",
            refreshingFiles: "正在刷新文件……",
            filesRefreshed: "知识库文件已刷新。",
            refreshingConversations: "正在刷新会话……",
            noConversationsYet: "暂无会话",
            untitledChat: "未命名会话",
            messageCount: "{count} 条消息",
            loadingConversation: "正在加载会话……",
            conversationLoaded: "会话已加载。",
            newChatReady: "新会话已就绪。",
            newChatMessage: "新会话已开始。请基于已向量化的知识库文件提问。",
            allEmbeddedFiles: "全部已向量化文件",
            scopedToDocument: "限定文档 #{id}{title}",
            questionScopeSet: "提问范围已限定到文档 ID {id}。",
            embeddingFile: "正在向量化文件……",
            missingDocumentId: "缺少文档 ID。",
            deletingFile: "正在删除文件……",
            documentDeleted: "文档已删除。",
            untitledFile: "未命名文件",
            fileTypeFallback: "文件",
            documentMeta: "id {id} · {type}",
            statusUploaded: "已上传",
            statusParsed: "已解析",
            statusChunked: "已分块",
            statusEmbedded: "已向量化",
            statusFailed: "失败",
            enterQuestion: "发送前请输入问题。",
            thinking: "正在思考……",
            noAnswerReturned: "未返回回答。",
            answerReady: "回答已就绪。",
            questionFailed: "提问失败。",
            asking: "正在提问……",
            chooseFileBeforeUpload: "上传前请选择文件。",
            uploadingFile: "正在上传文件……",
            uploadComplete: "上传完成。提问前请先点击向量化。",
            loadingAuditLogs: "正在加载审计日志……",
            auditLogsRefreshed: "审计日志已刷新。",
            details: "详情",
            audit: "审计",
            close: "关闭",
            noResourceId: "无资源 ID"
        },
        "en-US": {
            documentTitle: "Secure Vault AI",
            appTitle: "Secure Vault AI",
            appSubtitle: "Privacy-first Personal Knowledge Vault",
            languageLabel: "Language",
            languageSelectorLabel: "Choose interface language",
            knowledgeSidebarAria: "Knowledge sidebar",
            detailsPanelAria: "Details panel",
            authenticationAria: "Authentication",
            status: "Status",
            alreadyLoggedIn: "Already logged in",
            alreadyLoggedInWithUser: "Already logged in: {username}",
            pleaseSignIn: "Please sign in",
            register: "Register",
            login: "Login",
            logout: "Logout",
            username: "Username",
            email: "Email",
            password: "Password",
            usernamePlaceholder: "Enter username",
            emailPlaceholder: "Enter email",
            passwordPlaceholder: "Enter password",
            createAccount: "Create account",
            signIn: "Sign in",
            newChat: "New Chat",
            knowledgeFiles: "Knowledge Files",
            uploadFile: "Upload File",
            refresh: "Refresh",
            refreshConversations: "Refresh conversations",
            auditLogs: "Audit Logs",
            conversations: "Conversations",
            documents: "Documents",
            noDocumentsYet: "No documents yet",
            uploadKnowledgeFile: "Upload Knowledge File",
            selectFile: "Select file",
            file: "File",
            title: "Title",
            description: "Description",
            optionalTitle: "Optional title",
            upload: "Upload",
            uploadSuccess: "Upload success",
            pleaseSelectFile: "Please select a file",
            localRagWorkspace: "Local RAG workspace",
            knowledgeChat: "Knowledge Chat",
            chatSubtitle: "Ask questions based on your uploaded knowledge files.",
            welcomeMessage: "Upload a file, click Embed, then ask a question. Sources will appear under each answer.",
            questionPlaceholder: "Type your question...",
            questionAria: "Question",
            ask: "Ask",
            sending: "Sending...",
            answer: "Answer",
            you: "You",
            sources: "Sources",
            noSourcesReturned: "No sources returned.",
            noReadablePreview: "No readable preview was returned for this source.",
            source: "Source",
            sourceNumber: "Source {number}",
            document: "Document",
            documentId: "Document ID",
            position: "Position",
            chunkPositionValue: "chunk {index}",
            match: "Match",
            preview: "Preview",
            scoreUnavailable: "score unavailable",
            unknown: "unknown",
            action: "Action",
            resource: "Resource",
            resourceId: "Resource ID",
            success: "Success",
            failed: "Failed",
            message: "Message",
            createdAt: "Created At",
            noAuditLogsYet: "No audit logs yet",
            embed: "Embed",
            delete: "Delete",
            view: "View",
            confirmDelete: "Confirm delete?",
            confirmDeleteWithName: "Delete {name}?",
            deleteSuccess: "Delete success",
            embedSuccess: "Embed success",
            operationSucceeded: "Operation succeeded",
            operationFailed: "Operation failed",
            unauthorized: "Unauthorized. Please login again.",
            networkError: "Network error. Check whether the backend is running.",
            loading: "Loading...",
            copied: "Copied",
            requestFailed: "Request failed.",
            httpRequestFailed: "Request failed with HTTP {status}.",
            creatingAccount: "Creating account...",
            registrationComplete: "Registration complete. You can login now.",
            signingIn: "Signing in...",
            signedInStatus: "Signed in.",
            signedOutStatus: "Signed out.",
            signedOutMessage: "You are signed out. Login to use your private knowledge vault.",
            usernameEmailPasswordRequired: "Username, email, and password are required.",
            usernamePasswordRequired: "Username and password are required.",
            loginMissingToken: "Login response did not include a token.",
            refreshingFiles: "Refreshing files...",
            filesRefreshed: "Knowledge files refreshed.",
            refreshingConversations: "Refreshing conversations...",
            noConversationsYet: "No conversations yet.",
            untitledChat: "Untitled chat",
            messageCount: "{count} messages",
            loadingConversation: "Loading conversation...",
            conversationLoaded: "Conversation loaded.",
            newChatReady: "New chat ready.",
            newChatMessage: "New chat started. Ask a question about your embedded knowledge files.",
            allEmbeddedFiles: "All embedded files",
            scopedToDocument: "Scoped to #{id}{title}",
            questionScopeSet: "Question scope set to document id {id}.",
            embeddingFile: "Embedding file...",
            missingDocumentId: "Missing document id.",
            deletingFile: "Deleting file...",
            documentDeleted: "Document deleted.",
            untitledFile: "Untitled file",
            fileTypeFallback: "file",
            documentMeta: "id {id} · {type}",
            statusUploaded: "Uploaded",
            statusParsed: "Parsed",
            statusChunked: "Chunked",
            statusEmbedded: "Embedded",
            statusFailed: "Failed",
            enterQuestion: "Enter a question before sending.",
            thinking: "Thinking...",
            noAnswerReturned: "No answer returned.",
            answerReady: "Answer ready.",
            questionFailed: "Question failed.",
            asking: "Asking...",
            chooseFileBeforeUpload: "Choose a file before uploading.",
            uploadingFile: "Uploading file...",
            uploadComplete: "Upload complete. Click Embed before asking about this file.",
            loadingAuditLogs: "Loading audit logs...",
            auditLogsRefreshed: "Audit logs refreshed.",
            details: "Details",
            audit: "Audit",
            close: "Close",
            noResourceId: "no resource id"
        }
    };

    const state = {
        token: localStorage.getItem(tokenKey),
        username: localStorage.getItem(usernameKey),
        language: getCurrentLanguage(),
        selectedDocumentId: null,
        selectedDocumentTitle: "",
        currentConversationId: null,
        busy: false,
        sendInProgress: false,
        chatTitleKey: "knowledgeChat",
        detailsTitleKey: "audit"
    };

    const els = {};

    document.addEventListener("DOMContentLoaded", init);

    function init() {
        cacheElements();
        bindEvents();
        applyLanguage();
        updateAuthView();

        if (state.token) {
            loadWorkspace();
        }
    }

    function cacheElements() {
        [
            "languageSelect",
            "sessionLabel",
            "newChatButton",
            "refreshConversationsButton",
            "conversationList",
            "openUploadButton",
            "refreshDocumentsButton",
            "documentList",
            "auditButton",
            "logoutButton",
            "chatTitle",
            "selectedDocumentLabel",
            "statusArea",
            "messageList",
            "askForm",
            "questionInput",
            "topKInput",
            "sendButton",
            "detailsPanel",
            "detailsTitle",
            "detailsContent",
            "closeDetailsButton",
            "authOverlay",
            "loginForm",
            "loginUsername",
            "loginPassword",
            "registerForm",
            "registerUsername",
            "registerEmail",
            "registerPassword",
            "uploadModal",
            "closeUploadButton",
            "uploadForm",
            "fileInput",
            "fileTitleInput"
        ].forEach((id) => {
            els[id] = document.getElementById(id);
        });
    }

    function bindEvents() {
        on(els.languageSelect, "change", (event) => setLanguage(event.target.value));
        on(els.loginForm, "submit", login);
        on(els.registerForm, "submit", register);
        on(els.logoutButton, "click", logout);
        on(els.refreshDocumentsButton, "click", () => runAction(loadDocuments, "refreshingFiles"));
        on(els.refreshConversationsButton, "click", () => runAction(loadConversations, "refreshingConversations"));
        on(els.newChatButton, "click", startNewChat);
        on(els.askForm, "submit", ask);
        on(els.openUploadButton, "click", openUploadModal);
        on(els.closeUploadButton, "click", closeUploadModal);
        on(els.uploadForm, "submit", uploadFile);
        on(els.auditButton, "click", () => runAction(loadAuditLogs, "loadingAuditLogs"));
        on(els.closeDetailsButton, "click", closeDetailsPanel);
    }

    function on(element, eventName, handler) {
        if (element) {
            element.addEventListener(eventName, handler);
        }
    }

    function getCurrentLanguage() {
        const saved = localStorage.getItem(languageKey);
        return supportedLanguages.includes(saved) ? saved : defaultLanguage;
    }

    function setLanguage(lang) {
        const nextLanguage = supportedLanguages.includes(lang) ? lang : defaultLanguage;
        state.language = nextLanguage;
        localStorage.setItem(languageKey, nextLanguage);
        applyLanguage();
    }

    function t(key, params) {
        const dictionary = I18N[state.language] || I18N[defaultLanguage];
        const fallbackDictionary = I18N[defaultLanguage];
        const template = dictionary[key] || fallbackDictionary[key] || key;
        return format(template, params);
    }

    function format(template, params) {
        if (!params) {
            return template;
        }

        return String(template).replace(/\{(\w+)}/g, (match, name) => {
            if (Object.prototype.hasOwnProperty.call(params, name)) {
                return params[name] === null || params[name] === undefined ? "" : String(params[name]);
            }
            return match;
        });
    }

    function applyLanguage() {
        document.documentElement.lang = state.language;
        document.title = t("documentTitle");

        if (els.languageSelect) {
            els.languageSelect.value = state.language;
        }

        document.querySelectorAll("[data-i18n]").forEach((element) => {
            element.textContent = t(element.dataset.i18n, readI18nParams(element));
        });

        document.querySelectorAll("[data-i18n-placeholder]").forEach((element) => {
            element.placeholder = t(element.dataset.i18nPlaceholder, readI18nParams(element));
        });

        document.querySelectorAll("[data-i18n-title]").forEach((element) => {
            element.title = t(element.dataset.i18nTitle, readI18nParams(element));
        });

        document.querySelectorAll("[data-i18n-aria-label]").forEach((element) => {
            element.setAttribute("aria-label", t(element.dataset.i18nAriaLabel, readI18nParams(element)));
        });

        updateDynamicText();
    }

    function readI18nParams(element) {
        if (!element || !element.dataset.i18nParams) {
            return null;
        }

        try {
            return JSON.parse(element.dataset.i18nParams);
        } catch (error) {
            return null;
        }
    }

    function setLocalizedText(element, key, params) {
        if (!element) {
            return;
        }

        element.dataset.i18n = key;
        if (params) {
            element.dataset.i18nParams = JSON.stringify(params);
        } else {
            delete element.dataset.i18nParams;
        }
        element.textContent = t(key, params);
    }

    function setLocalizedStatus(key, type, params) {
        if (!els.statusArea) {
            return;
        }

        els.statusArea.dataset.i18nStatusKey = key;
        if (params) {
            els.statusArea.dataset.i18nParams = JSON.stringify(params);
        } else {
            delete els.statusArea.dataset.i18nParams;
        }
        setStatus(t(key, params), type, true);
    }

    function localizedError(key, params) {
        const error = new Error(t(key, params));
        error.i18nKey = key;
        error.i18nParams = params || null;
        return error;
    }

    function updateDynamicText() {
        updateAuthView();
        updateSelectedDocumentLabel();
        updateChatTitle();
        updateDetailsTitle();
        updateSendButtonLabel();
        updateStatusLanguage();
    }

    function updateStatusLanguage() {
        if (!els.statusArea || !els.statusArea.dataset.i18nStatusKey) {
            return;
        }

        const key = els.statusArea.dataset.i18nStatusKey;
        els.statusArea.textContent = t(key, readI18nParams(els.statusArea));
    }

    async function apiFetch(path, options) {
        const request = options || {};
        const headers = new Headers(request.headers || {});
        const init = {
            method: request.method || "GET",
            headers
        };

        if (request.body instanceof FormData) {
            init.body = request.body;
        } else if (request.body !== undefined && request.body !== null) {
            if (!headers.has("Content-Type")) {
                headers.set("Content-Type", "application/json; charset=utf-8");
            }
            init.body = typeof request.body === "string" ? request.body : JSON.stringify(request.body);
        }

        if (request.auth !== false && state.token) {
            headers.set("Authorization", "Bearer " + state.token);
        }

        let response;
        try {
            response = await fetch(path, init);
        } catch (error) {
            throw localizedError("networkError");
        }

        const raw = await response.text();
        const payload = parseJson(raw);

        if (response.status === 401) {
            handleUnauthorized();
            throw localizedError("unauthorized");
        }

        if (!response.ok) {
            const responseMessage = extractMessage(payload);
            if (responseMessage) {
                throw new Error(responseMessage);
            }
            throw localizedError("httpRequestFailed", { status: response.status });
        }

        if (isApiResponse(payload)) {
            if (payload.code !== 0) {
                if (payload.message) {
                    throw new Error(payload.message);
                }
                throw localizedError("requestFailed");
            }
            return payload.data;
        }

        return payload;
    }

    function parseJson(raw) {
        if (!raw) {
            return null;
        }

        try {
            return JSON.parse(raw);
        } catch (error) {
            return raw;
        }
    }

    function isApiResponse(payload) {
        return payload &&
            typeof payload === "object" &&
            Object.prototype.hasOwnProperty.call(payload, "code") &&
            Object.prototype.hasOwnProperty.call(payload, "message") &&
            Object.prototype.hasOwnProperty.call(payload, "data");
    }

    function extractMessage(payload) {
        if (!payload) {
            return "";
        }

        if (typeof payload === "string") {
            return payload;
        }

        return payload.message || payload.error || "";
    }

    async function runAction(action, loadingKey, loadingParams) {
        if (state.busy) {
            return;
        }

        setBusy(true);
        if (loadingKey) {
            setLocalizedStatus(loadingKey, "loading", loadingParams);
        }

        try {
            await action();
        } catch (error) {
            if (error.i18nKey) {
                setLocalizedStatus(error.i18nKey, "error", error.i18nParams);
            } else {
                setStatus(error.message || t("requestFailed"), "error");
            }
        } finally {
            setBusy(false);
        }
    }

    function setBusy(isBusy) {
        state.busy = isBusy;
        document.querySelectorAll("button").forEach((button) => {
            button.disabled = isBusy;
        });
        updateSendButtonLabel();
    }

    function updateSendButtonLabel() {
        setLocalizedText(els.sendButton, state.sendInProgress ? "sending" : "ask");
    }

    function setStatus(message, type, keepI18n) {
        if (!els.statusArea) {
            return;
        }

        if (!keepI18n) {
            delete els.statusArea.dataset.i18nStatusKey;
            delete els.statusArea.dataset.i18nParams;
        }

        els.statusArea.textContent = message || "";
        els.statusArea.className = "status-area";

        if (message) {
            els.statusArea.classList.add(type || "info");
        }
    }

    function updateAuthView() {
        const signedIn = Boolean(state.token);

        if (els.authOverlay) {
            els.authOverlay.classList.toggle("hidden", signedIn);
        }

        if (els.sessionLabel) {
            els.sessionLabel.textContent = signedIn
                ? (state.username ? t("alreadyLoggedInWithUser", { username: state.username }) : t("alreadyLoggedIn"))
                : t("pleaseSignIn");
        }
    }

    function handleUnauthorized() {
        state.token = null;
        state.username = null;
        state.currentConversationId = null;
        localStorage.removeItem(tokenKey);
        localStorage.removeItem(usernameKey);
        updateAuthView();
    }

    async function register(event) {
        event.preventDefault();

        const username = valueOf(els.registerUsername);
        const email = valueOf(els.registerEmail);
        const password = valueOf(els.registerPassword);

        if (!username || !email || !password) {
            setLocalizedStatus("usernameEmailPasswordRequired", "error");
            return;
        }

        await runAction(async () => {
            await apiFetch("/api/auth/register", {
                method: "POST",
                auth: false,
                body: { username, email, password }
            });

            if (els.registerPassword) {
                els.registerPassword.value = "";
            }

            setLocalizedStatus("registrationComplete", "success");
        }, "creatingAccount");
    }

    async function login(event) {
        event.preventDefault();

        const username = valueOf(els.loginUsername);
        const password = valueOf(els.loginPassword);

        if (!username || !password) {
            setLocalizedStatus("usernamePasswordRequired", "error");
            return;
        }

        await runAction(async () => {
            const data = await apiFetch("/api/auth/login", {
                method: "POST",
                auth: false,
                body: { username, password }
            });

            if (!data || !data.token) {
                throw localizedError("loginMissingToken");
            }

            state.token = data.token;
            state.username = username;
            localStorage.setItem(tokenKey, data.token);
            localStorage.setItem(usernameKey, username);

            if (els.loginPassword) {
                els.loginPassword.value = "";
            }

            updateAuthView();
            await loadWorkspace();
            setLocalizedStatus("signedInStatus", "success");
        }, "signingIn");
    }

    function logout() {
        handleUnauthorized();
        state.selectedDocumentId = null;
        state.selectedDocumentTitle = "";
        state.chatTitleKey = "knowledgeChat";
        clearMessages();
        appendMessage("assistant", t("signedOutMessage"), [], { bodyKey: "signedOutMessage" });
        renderDocuments([]);
        renderConversations([]);
        updateSelectedDocumentLabel();
        updateChatTitle();
        setLocalizedStatus("signedOutStatus", "info");
    }

    async function loadWorkspace() {
        updateAuthView();
        await Promise.all([loadDocuments(), loadConversations()]);
    }

    async function loadDocuments() {
        const documents = await apiFetch("/api/documents");
        renderDocuments(Array.isArray(documents) ? documents : []);
        setLocalizedStatus("filesRefreshed", "success");
    }

    function renderDocuments(documents) {
        if (!els.documentList) {
            return;
        }

        els.documentList.replaceChildren();

        if (!documents.length) {
            els.documentList.appendChild(emptyState("noDocumentsYet"));
            return;
        }

        documents.forEach((doc) => {
            const item = document.createElement("article");
            item.className = "document-item";
            item.tabIndex = 0;
            if (doc.id === state.selectedDocumentId) {
                item.classList.add("selected");
            }

            const main = document.createElement("button");
            main.type = "button";
            main.className = "document-main";
            main.addEventListener("click", () => selectDocument(doc));

            const title = document.createElement("span");
            title.className = "document-title";
            title.textContent = doc.title || doc.originalFilename || t("untitledFile");

            const meta = document.createElement("span");
            meta.className = "document-meta";
            setLocalizedText(meta, "documentMeta", {
                id: doc.id,
                type: doc.fileType || t("fileTypeFallback")
            });

            main.append(title, meta);

            const badge = document.createElement("span");
            badge.className = "badge " + badgeClass(doc.status);
            setDocumentStatusText(badge, doc.status);

            const actions = document.createElement("div");
            actions.className = "document-actions";

            const embedButton = document.createElement("button");
            embedButton.type = "button";
            embedButton.className = "mini-button";
            setLocalizedText(embedButton, "embed");
            embedButton.addEventListener("click", () => runAction(() => embedDocument(doc.id), "embeddingFile"));

            const deleteButton = document.createElement("button");
            deleteButton.type = "button";
            deleteButton.className = "mini-button danger-text";
            setLocalizedText(deleteButton, "delete");
            deleteButton.addEventListener("click", () => deleteDocument(doc));

            actions.append(embedButton, deleteButton);
            item.append(main, badge, actions);
            els.documentList.appendChild(item);
        });
    }

    function setDocumentStatusText(element, status) {
        const key = documentStatusKey(status);
        if (key) {
            element.title = status || "";
            setLocalizedText(element, key);
            return;
        }

        element.textContent = status || t("unknown");
    }

    function documentStatusKey(status) {
        const value = String(status || "").toUpperCase();

        if (value.includes("FAILED")) {
            return "statusFailed";
        }

        if (value.includes("EMBEDDED")) {
            return "statusEmbedded";
        }

        if (value.includes("CHUNKED")) {
            return "statusChunked";
        }

        if (value.includes("PARSED")) {
            return "statusParsed";
        }

        if (value.includes("UPLOADED")) {
            return "statusUploaded";
        }

        return "";
    }

    function selectDocument(doc) {
        state.selectedDocumentId = doc.id || null;
        state.selectedDocumentTitle = doc.title || doc.originalFilename || "";
        updateSelectedDocumentLabel();
        loadDocuments()
            .then(() => setLocalizedStatus("questionScopeSet", "info", { id: state.selectedDocumentId }))
            .catch((error) => setStatus(error.message, "error"));
    }

    async function embedDocument(documentId) {
        if (!documentId) {
            throw localizedError("missingDocumentId");
        }

        await apiFetch("/api/documents/" + encodeURIComponent(documentId) + "/embed", {
            method: "POST"
        });

        await loadDocuments();
        setLocalizedStatus("embedSuccess", "success");
    }

    function deleteDocument(doc) {
        const label = doc.title || doc.originalFilename || (t("document") + " " + doc.id);
        if (!confirm(t("confirmDeleteWithName", { name: label }))) {
            return;
        }

        runAction(async () => {
            await apiFetch("/api/documents/" + encodeURIComponent(doc.id), {
                method: "DELETE"
            });

            if (state.selectedDocumentId === doc.id) {
                state.selectedDocumentId = null;
                state.selectedDocumentTitle = "";
                updateSelectedDocumentLabel();
            }

            await loadDocuments();
            setLocalizedStatus("deleteSuccess", "success");
        }, "deletingFile");
    }

    async function loadConversations() {
        const conversations = await apiFetch("/api/conversations");
        renderConversations(Array.isArray(conversations) ? conversations : []);
    }

    function renderConversations(conversations) {
        if (!els.conversationList) {
            return;
        }

        els.conversationList.replaceChildren();

        if (!conversations.length) {
            els.conversationList.appendChild(emptyState("noConversationsYet"));
            return;
        }

        conversations.forEach((conversation) => {
            const item = document.createElement("button");
            item.type = "button";
            item.className = "conversation-item";
            if (conversation.conversationId === state.currentConversationId) {
                item.classList.add("selected");
            }

            const title = document.createElement("span");
            title.textContent = conversation.title || t("untitledChat");

            const meta = document.createElement("small");
            setLocalizedText(meta, "messageCount", { count: conversation.messageCount || 0 });

            item.append(title, meta);
            item.addEventListener("click", () => runAction(() => loadConversationMessages(conversation.conversationId), "loadingConversation"));
            els.conversationList.appendChild(item);
        });
    }

    async function loadConversationMessages(conversationId) {
        if (!conversationId) {
            return;
        }

        const data = await apiFetch("/api/conversations/" + encodeURIComponent(conversationId) + "/messages");
        state.currentConversationId = data.conversationId || conversationId;
        state.chatTitleKey = null;
        clearMessages();

        if (els.chatTitle) {
            delete els.chatTitle.dataset.i18n;
            delete els.chatTitle.dataset.i18nParams;
            els.chatTitle.textContent = data.title || t("knowledgeChat");
        }

        const messages = Array.isArray(data.messages) ? data.messages : [];
        messages.forEach((message) => {
            const role = String(message.role || "").toUpperCase() === "USER" ? "user" : "assistant";
            appendMessage(role, message.content || "", message.sources || []);
        });

        await loadConversations();
        setLocalizedStatus("conversationLoaded", "success");
    }

    function startNewChat() {
        state.currentConversationId = null;
        state.chatTitleKey = "knowledgeChat";
        updateChatTitle();
        clearMessages();
        appendMessage("assistant", t("newChatMessage"), [], { bodyKey: "newChatMessage" });
        setLocalizedStatus("newChatReady", "info");
        loadConversations().catch((error) => setStatus(error.message, "error"));
    }

    async function ask(event) {
        event.preventDefault();

        const question = valueOf(els.questionInput);
        if (!question) {
            setLocalizedStatus("enterQuestion", "error");
            return;
        }

        state.sendInProgress = true;
        updateSendButtonLabel();

        try {
            await runAction(async () => {
                appendMessage("user", question, null);

                const loadingMessage = appendMessage("assistant", t("thinking"), null, { bodyKey: "thinking" });
                loadingMessage.classList.add("loading-message");

                const body = {
                    question,
                    topK: normalizeTopK()
                };

                if (state.selectedDocumentId) {
                    body.documentId = state.selectedDocumentId;
                }

                if (state.currentConversationId) {
                    body.conversationId = state.currentConversationId;
                }

                try {
                    const data = await apiFetch("/api/chat/ask", {
                        method: "POST",
                        body
                    });

                    loadingMessage.remove();

                    if (data && data.conversationId) {
                        state.currentConversationId = data.conversationId;
                    }

                    appendMessage("assistant", data && data.answer ? data.answer : t("noAnswerReturned"), data ? data.sources : [], data && data.answer ? null : { bodyKey: "noAnswerReturned" });

                    if (els.questionInput) {
                        els.questionInput.value = "";
                    }

                    await loadConversations();
                    setLocalizedStatus("answerReady", "success");
                } catch (error) {
                    loadingMessage.remove();
                    appendMessage("assistant", error.message || t("questionFailed"), []);
                    throw error;
                }
            }, "asking");
        } finally {
            state.sendInProgress = false;
            updateSendButtonLabel();
        }
    }

    function appendMessage(role, content, sources, options) {
        const list = els.messageList;
        if (!list) {
            return document.createElement("article");
        }

        const article = document.createElement("article");
        article.className = "message " + (role === "user" ? "user-message" : "assistant-message");

        const meta = document.createElement("div");
        meta.className = "message-meta";
        setLocalizedText(meta, role === "user" ? "you" : "answer");

        const body = document.createElement("div");
        body.className = "message-body";
        if (options && options.bodyKey) {
            setLocalizedText(body, options.bodyKey, options.bodyParams);
        } else {
            body.textContent = content || "";
        }

        article.append(meta, body);

        if (sources !== null && sources !== undefined) {
            article.appendChild(renderSources(sources));
        }

        list.appendChild(article);
        list.scrollTop = list.scrollHeight;
        return article;
    }

    function renderSources(sources) {
        const wrapper = document.createElement("div");
        wrapper.className = "source-list";

        const title = document.createElement("div");
        title.className = "source-title";
        setLocalizedText(title, "sources");
        wrapper.appendChild(title);

        if (!Array.isArray(sources) || sources.length === 0) {
            const empty = document.createElement("div");
            empty.className = "source-card empty";
            setLocalizedText(empty, "noSourcesReturned");
            wrapper.appendChild(empty);
            return wrapper;
        }

        sources.forEach((source, index) => {
            const card = document.createElement("article");
            card.className = "source-card";

            const heading = document.createElement("div");
            heading.className = "source-card-title";
            if (source.sourceId) {
                heading.textContent = source.sourceId;
            } else {
                setLocalizedText(heading, "sourceNumber", { number: index + 1 });
            }

            const chunkIndex = getSourceChunkIndex(source);
            const score = formatScore(source.score);
            const meta = document.createElement("div");
            meta.className = "source-meta";
            meta.append(
                sourceMetaItem("document", getSourceTitle(source)),
                sourceMetaItem("documentId", getSourceDocumentId(source), getSourceDocumentId(source) ? null : "unknown"),
                chunkIndex === null ? sourceMetaItem("position", "", "unknown") : sourceMetaItem("position", "", "chunkPositionValue", { index: chunkIndex }),
                score.key ? sourceMetaItem("match", "", score.key) : sourceMetaItem("match", score.text)
            );

            const previewLabel = document.createElement("div");
            previewLabel.className = "source-preview-label";
            setLocalizedText(previewLabel, "preview");

            const previewInfo = getSourcePreview(source);
            const preview = document.createElement("p");
            preview.className = "source-preview";
            if (previewInfo.key) {
                setLocalizedText(preview, previewInfo.key);
            } else {
                preview.textContent = previewInfo.text;
            }

            card.append(heading, meta, previewLabel, preview);
            wrapper.appendChild(card);
        });

        return wrapper;
    }

    function sourceMetaItem(labelKey, value, valueKey, valueParams) {
        const item = document.createElement("span");
        const name = document.createElement("strong");
        const label = document.createElement("span");
        setLocalizedText(label, labelKey);
        name.append(label, document.createTextNode(": "));
        item.append(name);

        if (valueKey) {
            const valueNode = document.createElement("span");
            setLocalizedText(valueNode, valueKey, valueParams);
            item.append(valueNode);
        } else {
            item.append(document.createTextNode(safeText(value)));
        }

        return item;
    }

    function openUploadModal() {
        if (els.uploadModal) {
            els.uploadModal.classList.remove("hidden");
        }
    }

    function closeUploadModal() {
        if (els.uploadModal) {
            els.uploadModal.classList.add("hidden");
        }
    }

    async function uploadFile(event) {
        event.preventDefault();

        const file = els.fileInput && els.fileInput.files ? els.fileInput.files[0] : null;
        if (!file) {
            setLocalizedStatus("pleaseSelectFile", "error");
            return;
        }

        await runAction(async () => {
            const form = new FormData();
            form.append("file", file);

            const title = valueOf(els.fileTitleInput);
            if (title) {
                form.append("title", title);
            }

            await apiFetch("/api/documents/upload", {
                method: "POST",
                body: form
            });

            if (els.uploadForm) {
                els.uploadForm.reset();
            }

            closeUploadModal();
            await loadDocuments();
            setLocalizedStatus("uploadComplete", "success");
        }, "uploadingFile");
    }

    async function loadAuditLogs() {
        openDetailsPanel("auditLogs");
        const data = await apiFetch("/api/me/audit-logs?size=20");
        renderAuditLogs(data && Array.isArray(data.items) ? data.items : []);
        setLocalizedStatus("auditLogsRefreshed", "success");
    }

    function openDetailsPanel(titleKey) {
        state.detailsTitleKey = titleKey || "details";
        updateDetailsTitle();

        if (els.detailsPanel) {
            els.detailsPanel.classList.add("open");
        }
    }

    function closeDetailsPanel() {
        if (els.detailsPanel) {
            els.detailsPanel.classList.remove("open");
        }
    }

    function renderAuditLogs(items) {
        if (!els.detailsContent) {
            return;
        }

        els.detailsContent.replaceChildren();

        if (!items.length) {
            els.detailsContent.appendChild(emptyState("noAuditLogsYet"));
            return;
        }

        items.forEach((item) => {
            const event = document.createElement("article");
            event.className = "audit-event";

            const top = document.createElement("div");
            top.className = "audit-top";

            const actionWrap = document.createElement("div");
            actionWrap.className = "audit-action";
            const actionLabel = document.createElement("span");
            actionLabel.className = "audit-label";
            setLocalizedText(actionLabel, "action");
            const action = document.createElement("strong");
            action.textContent = item.action || "UNKNOWN_ACTION";
            actionWrap.append(actionLabel, document.createTextNode(": "), action);

            const badge = document.createElement("span");
            badge.className = "badge " + (item.success ? "success" : "failed");
            setLocalizedText(badge, item.success ? "success" : "failed");

            top.append(actionWrap, badge);

            const meta = document.createElement("div");
            meta.className = "audit-meta";
            meta.append(
                auditMetaItem("resource", item.resourceType || t("unknown")),
                auditMetaItem("resourceId", item.resourceId ? "id " + item.resourceId : t("noResourceId")),
                auditMetaItem("createdAt", item.createdAt || t("unknown"))
            );

            const message = document.createElement("p");
            const messageLabel = document.createElement("strong");
            const messageLabelText = document.createElement("span");
            setLocalizedText(messageLabelText, "message");
            messageLabel.append(messageLabelText, document.createTextNode(": "));
            message.append(messageLabel, document.createTextNode(item.message || ""));

            event.append(top, meta, message);
            els.detailsContent.appendChild(event);
        });
    }

    function auditMetaItem(labelKey, value) {
        const item = document.createElement("span");
        const label = document.createElement("strong");
        const labelText = document.createElement("span");
        setLocalizedText(labelText, labelKey);
        label.append(labelText, document.createTextNode(": "));
        item.append(label, document.createTextNode(safeText(value)));
        return item;
    }

    function clearMessages() {
        if (els.messageList) {
            els.messageList.replaceChildren();
        }
    }

    function updateSelectedDocumentLabel() {
        if (!els.selectedDocumentLabel) {
            return;
        }

        els.selectedDocumentLabel.textContent = state.selectedDocumentId
            ? t("scopedToDocument", {
                id: state.selectedDocumentId,
                title: state.selectedDocumentTitle ? " " + state.selectedDocumentTitle : ""
            })
            : t("allEmbeddedFiles");
    }

    function updateChatTitle() {
        if (!els.chatTitle || !state.chatTitleKey) {
            return;
        }

        setLocalizedText(els.chatTitle, state.chatTitleKey);
    }

    function updateDetailsTitle() {
        if (!els.detailsTitle || !state.detailsTitleKey) {
            return;
        }

        setLocalizedText(els.detailsTitle, state.detailsTitleKey);
    }

    function normalizeTopK() {
        const value = Number.parseInt(valueOf(els.topKInput), 10);
        if (Number.isNaN(value)) {
            return 5;
        }
        return Math.max(1, Math.min(20, value));
    }

    function emptyState(key, params) {
        const node = document.createElement("div");
        node.className = "empty-state";
        setLocalizedText(node, key, params);
        return node;
    }

    function valueOf(element) {
        return element && typeof element.value === "string" ? element.value.trim() : "";
    }

    function badgeClass(status) {
        const value = String(status || "").toUpperCase();

        if (value.includes("FAILED")) {
            return "failed";
        }

        if (value.includes("EMBEDDED") || value.includes("CHUNKED") || value.includes("SUCCESS")) {
            return "success";
        }

        return "status";
    }

    function formatScore(score) {
        const value = Number(score);
        if (Number.isNaN(value)) {
            return { text: t("scoreUnavailable"), key: "scoreUnavailable" };
        }

        if (value >= 0 && value <= 1) {
            return { text: (value * 100).toFixed(1) + "% (" + value.toFixed(3) + ")" };
        }

        return { text: value.toFixed(3) };
    }

    function safeText(value) {
        return value === null || value === undefined || value === "" ? "-" : String(value);
    }

    function getSourceTitle(source) {
        return source.documentTitle ||
            source.title ||
            source.filename ||
            source.originalFilename ||
            t("unknown");
    }

    function getSourceDocumentId(source) {
        return source.documentId ?? source.docId ?? null;
    }

    function getSourceChunkIndex(source) {
        return source.chunkIndex ?? source.chunk ?? null;
    }

    function getSourcePreview(source) {
        const previewText = source.contentPreview ||
            source.preview ||
            source.snippet ||
            source.contentPreviewText ||
            source.textPreview ||
            source.chunkPreview ||
            "";
        const normalized = normalizePreviewText(previewText);

        if (!normalized || isUnreadablePreview(normalized)) {
            return { text: t("noReadablePreview"), key: "noReadablePreview" };
        }

        return { text: truncate(normalized, 500) };
    }

    function normalizePreviewText(value) {
        return String(value || "")
            .replace(/\s+/g, " ")
            .trim();
    }

    function isUnreadablePreview(value) {
        return /^[\d\s.,;:|/\\_-]+$/.test(value);
    }

    function truncate(value, maxLength) {
        const text = String(value || "");
        if (text.length <= maxLength) {
            return text;
        }

        return text.slice(0, maxLength) + "...";
    }
})();
