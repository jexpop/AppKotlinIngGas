@file:OptIn(ExperimentalMaterial3Api::class)

package com.jexpop.appkotlininggas.ui.screens.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jexpop.appkotlininggas.R
import com.jexpop.appkotlininggas.data.model.CategoryGroup
import com.jexpop.appkotlininggas.data.model.Periodicity
import com.jexpop.appkotlininggas.data.model.RuleType
import com.jexpop.appkotlininggas.data.repository.CategorizationException
import com.jexpop.appkotlininggas.data.repository.CategorizationRule

@Composable
fun CategoriesScreen(
    modifier: Modifier = Modifier,
    viewModel: CategoriesViewModel = viewModel(factory = CategoriesViewModel.Factory)
) {
    val groups by viewModel.groups.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val exceptions by viewModel.exceptions.collectAsState()
    val state by viewModel.state.collectAsState()
    val periodicities by viewModel.periodicities.collectAsState()
    val ruleTypes by viewModel.ruleTypes.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val tabs = listOf(
        stringResource(R.string.categories_groups),
        stringResource(R.string.categories_rules)
    )

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> GroupsTab(
                groups = groups,
                periodicities = periodicities,
                onAdd = { viewModel.addGroup(it) },
                onEdit = { viewModel.updateGroup(it) },
                onDelete = { viewModel.deleteGroup(it) { msg -> errorMessage = msg } }
            )
            1 -> RulesTab(
                rules = rules,
                exceptions = exceptions,
                groups = groups,
                ruleTypes = ruleTypes,
                onAddRule = { viewModel.addRule(it) },
                onEditRule = { viewModel.updateRule(it) },
                onDeleteRule = { viewModel.deleteRule(it) },
                onAddException = { viewModel.addException(it) },
                onEditException = { viewModel.updateException(it) },
                onDeleteException = { viewModel.deleteException(it) }
            )
        }
    }

    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text(stringResource(R.string.error_unknown)) },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            }
        )
    }
}

fun buildFlatGroupList(
    groups: List<CategoryGroup>,
    expandedIds: Set<Int>,
    parentId: Int? = null,
    level: Int = 0
): List<Pair<CategoryGroup, Int>> {
    val result = mutableListOf<Pair<CategoryGroup, Int>>()
    groups.filter { it.parentId == parentId }.forEach { group ->
        result.add(Pair(group, level))
        if (group.id in expandedIds) {
            result.addAll(buildFlatGroupList(groups, expandedIds, group.id, level + 1))
        }
    }
    return result
}

@Composable
fun GroupsTab(
    groups: List<CategoryGroup>,
    periodicities: List<Periodicity>,
    onAdd: (CategoryGroup) -> Unit,
    onEdit: (CategoryGroup) -> Unit,
    onDelete: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<CategoryGroup?>(null) }
    var expandedIds by remember { mutableStateOf(setOf<Int>()) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (groups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.categories_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val flatList = buildFlatGroupList(groups, expandedIds)
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(flatList, key = { it.first.id ?: 0 }) { (group, level) ->
                    val hasChildren = groups.any { it.parentId == group.id }
                    val isExpanded = group.id in expandedIds
                    GroupTreeItem(
                        group = group,
                        hasChildren = hasChildren,
                        isExpanded = isExpanded,
                        periodicities = periodicities,
                        level = level,
                        onToggle = {
                            expandedIds = if (isExpanded)
                                expandedIds - group.id!!
                            else
                                expandedIds + group.id!!
                        },
                        onEdit = {
                            editingGroup = it
                            showDialog = true
                        },
                        onDelete = onDelete
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingGroup = null
                showDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.categories_add_group))
        }
    }

    if (showDialog) {
        GroupDialog(
            group = editingGroup,
            groups = groups,
            periodicities = periodicities,
            onConfirm = { group ->
                if (editingGroup == null) onAdd(group) else onEdit(group)
                showDialog = false
                editingGroup = null
            },
            onDismiss = {
                showDialog = false
                editingGroup = null
            }
        )
    }
}

@Composable
fun GroupTreeItem(
    group: CategoryGroup,
    hasChildren: Boolean,
    isExpanded: Boolean,
    periodicities: List<Periodicity>,
    level: Int,
    onToggle: () -> Unit,
    onEdit: (CategoryGroup) -> Unit,
    onDelete: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (level * 16).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasChildren) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowDown
                    else Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }

        Column(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
            Text(
                text = group.description,
                style = if (level == 0) MaterialTheme.typography.titleSmall
                else MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                group.periodicity?.let { p ->
                    if (p != "N") {
                        val desc = periodicities.firstOrNull { it.id == p }?.description ?: p
                        Text(
                            text = "($desc)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                group.flowType?.let {
                    Text(
                        text = if (it == "H") "↑" else "↓",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (it == "H") MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        IconButton(onClick = { onEdit(group) }) {
            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.categories_edit_group))
        }
        IconButton(onClick = { group.id?.let { onDelete(it) } }) {
            Icon(Icons.Filled.Delete, contentDescription = null)
        }
    }
}

@Composable
fun GroupDialog(
    group: CategoryGroup?,
    groups: List<CategoryGroup>,
    periodicities: List<Periodicity>,
    onConfirm: (CategoryGroup) -> Unit,
    onDismiss: () -> Unit
) {
    var description by remember { mutableStateOf(group?.description ?: "") }
    var flowType by remember { mutableStateOf(group?.flowType ?: "D") }
    var periodicity by remember { mutableStateOf(group?.periodicity ?: "N") }
    var parentId by remember { mutableStateOf(group?.parentId) }
    var showParentPicker by remember { mutableStateOf(false) }
    var showPeriodicityPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (group == null) R.string.categories_add_group
                    else R.string.categories_edit_group
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.banks_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Grupo padre",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { showParentPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = groups.firstOrNull { it.id == parentId }?.description
                            ?: "Sin padre (raíz)"
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = flowType == "D",
                        onClick = { flowType = "D" },
                        label = { Text("Gasto") }
                    )
                    FilterChip(
                        selected = flowType == "H",
                        onClick = { flowType = "H" },
                        label = { Text("Ingreso") }
                    )
                }

                Text(
                    text = "Periodicidad",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { showPeriodicityPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = periodicities.firstOrNull { it.id == periodicity }?.description
                            ?: periodicity
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        CategoryGroup(
                            id = group?.id,
                            parentId = parentId,
                            description = description,
                            periodicity = periodicity,
                            sortOrder = "",
                            flowType = flowType
                        )
                    )
                },
                enabled = description.isNotBlank()
            ) {
                Text(stringResource(R.string.dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )

    if (showParentPicker) {
        AlertDialog(
            onDismissRequest = { showParentPicker = false },
            title = { Text("Selecciona grupo padre") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Button(
                        onClick = {
                            parentId = null
                            showParentPicker = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sin padre (raíz)")
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    groups.filter { it.id != group?.id }.forEach { g ->
                        TextButton(
                            onClick = {
                                parentId = g.id
                                showParentPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(g.description)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showPeriodicityPicker) {
        AlertDialog(
            onDismissRequest = { showPeriodicityPicker = false },
            title = { Text("Selecciona periodicidad") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    periodicities.forEach { p ->
                        TextButton(
                            onClick = {
                                periodicity = p.id
                                showPeriodicityPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(p.description)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun RulesTab(
    rules: List<CategorizationRule>,
    exceptions: List<CategorizationException>,
    groups: List<CategoryGroup>,
    ruleTypes: List<RuleType>,
    onAddRule: (CategorizationRule) -> Unit,
    onEditRule: (CategorizationRule) -> Unit,
    onDeleteRule: (Int) -> Unit,
    onAddException: (CategorizationException) -> Unit,
    onEditException: (CategorizationException) -> Unit,
    onDeleteException: (Int) -> Unit
) {
    var selectedSubTab by remember { mutableStateOf(0) }
    val subTabs = listOf(
        stringResource(R.string.categories_auto_rules),
        stringResource(R.string.categories_manual_rules)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedSubTab) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedSubTab) {
            0 -> AutoRulesTab(
                rules = rules,
                groups = groups,
                ruleTypes = ruleTypes,
                onAdd = onAddRule,
                onEdit = onEditRule,
                onDelete = onDeleteRule
            )
            1 -> ExceptionsTab(
                exceptions = exceptions,
                groups = groups,
                onAdd = onAddException,
                onEdit = onEditException,
                onDelete = onDeleteException
            )
        }
    }
}

@Composable
fun AutoRulesTab(
    rules: List<CategorizationRule>,
    groups: List<CategoryGroup>,
    ruleTypes: List<RuleType>,
    onAdd: (CategorizationRule) -> Unit,
    onEdit: (CategorizationRule) -> Unit,
    onDelete: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<CategorizationRule?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (rules.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.categories_rules_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rules) { rule ->
                    RuleItem(
                        rule = rule,
                        groups = groups,
                        ruleTypes = ruleTypes,
                        onEdit = {
                            editingRule = it
                            showDialog = true
                        },
                        onDelete = { rule.id?.let { id -> onDelete(id) } }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingRule = null
                showDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.categories_add_rule))
        }
    }

    if (showDialog) {
        RuleDialog(
            rule = editingRule,
            groups = groups,
            ruleTypes = ruleTypes,
            onConfirm = { rule ->
                if (editingRule == null) onAdd(rule) else onEdit(rule)
                showDialog = false
                editingRule = null
            },
            onDismiss = {
                showDialog = false
                editingRule = null
            }
        )
    }
}

@Composable
fun RuleItem(
    rule: CategorizationRule,
    groups: List<CategoryGroup>,
    ruleTypes: List<RuleType>,
    onEdit: (CategorizationRule) -> Unit,
    onDelete: () -> Unit
) {
    val groupName = groups.firstOrNull { it.id == rule.group_id }?.description ?: "?"
    val ruleTypeName = ruleTypes.firstOrNull { it.id == rule.rule_type }?.description ?: "Tipo ${rule.rule_type}"
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.value1 ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$ruleTypeName → $groupName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(rule.value2, rule.value3, rule.value4)
                    .filterNotNull()
                    .joinToString(" | ")
                    .takeIf { it.isNotBlank() }
                    ?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
            }
            IconButton(onClick = { onEdit(rule) }) {
                Icon(Icons.Filled.Edit, contentDescription = null)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = null)
            }
        }
    }
}

@Composable
fun RuleDialog(
    rule: CategorizationRule?,
    groups: List<CategoryGroup>,
    ruleTypes: List<RuleType>,
    onConfirm: (CategorizationRule) -> Unit,
    onDismiss: () -> Unit
) {
    var ruleType by remember { mutableStateOf(rule?.rule_type ?: 1) }
    var groupId by remember { mutableStateOf(rule?.group_id ?: 0) }
    var value1 by remember { mutableStateOf(rule?.value1 ?: "") }
    var value2 by remember { mutableStateOf(rule?.value2 ?: "") }
    var value3 by remember { mutableStateOf(rule?.value3 ?: "") }
    var value4 by remember { mutableStateOf(rule?.value4 ?: "") }
    var showGroupPicker by remember { mutableStateOf(false) }
    var showRuleTypePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (rule == null) R.string.categories_add_rule
                    else R.string.categories_edit_rule
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tipo de regla",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { showRuleTypePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = ruleTypes.firstOrNull { it.id == ruleType }?.description
                            ?: "Selecciona tipo"
                    )
                }

                Text(
                    text = "Grupo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { showGroupPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = groups.firstOrNull { it.id == groupId }?.description
                            ?: "Selecciona grupo"
                    )
                }

                OutlinedTextField(
                    value = value1,
                    onValueChange = { value1 = it },
                    label = { Text("Valor 1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (ruleType in listOf(4, 5, 6, 7)) {
                    OutlinedTextField(
                        value = value2,
                        onValueChange = { value2 = it },
                        label = { Text("Valor 2") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (ruleType in listOf(5, 6, 7)) {
                    OutlinedTextField(
                        value = value3,
                        onValueChange = { value3 = it },
                        label = { Text("Valor 3") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (ruleType == 7) {
                    OutlinedTextField(
                        value = value4,
                        onValueChange = { value4 = it },
                        label = { Text("Valor 4") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        CategorizationRule(
                            id = rule?.id,
                            rule_type = ruleType,
                            group_id = groupId,
                            value1 = value1.takeIf { it.isNotBlank() },
                            value2 = value2.takeIf { it.isNotBlank() },
                            value3 = value3.takeIf { it.isNotBlank() },
                            value4 = value4.takeIf { it.isNotBlank() }
                        )
                    )
                },
                enabled = value1.isNotBlank() && groupId > 0
            ) {
                Text(stringResource(R.string.dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )

    if (showRuleTypePicker) {
        AlertDialog(
            onDismissRequest = { showRuleTypePicker = false },
            title = { Text("Selecciona tipo de regla") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    ruleTypes.forEach { rt ->
                        TextButton(
                            onClick = {
                                ruleType = rt.id
                                showRuleTypePicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(rt.description)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showGroupPicker) {
        AlertDialog(
            onDismissRequest = { showGroupPicker = false },
            title = { Text("Selecciona grupo") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    groups.forEach { g ->
                        TextButton(
                            onClick = {
                                groupId = g.id ?: 0
                                showGroupPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(g.description)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun ExceptionsTab(
    exceptions: List<CategorizationException>,
    groups: List<CategoryGroup>,
    onAdd: (CategorizationException) -> Unit,
    onEdit: (CategorizationException) -> Unit,
    onDelete: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingException by remember { mutableStateOf<CategorizationException?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (exceptions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.categories_exceptions_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(exceptions) { exception ->
                    ExceptionItem(
                        exception = exception,
                        groups = groups,
                        onEdit = {
                            editingException = it
                            showDialog = true
                        },
                        onDelete = { exception.id?.let { id -> onDelete(id) } }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingException = null
                showDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.categories_add_exception))
        }
    }

    if (showDialog) {
        ExceptionDialog(
            exception = editingException,
            groups = groups,
            onConfirm = { exception ->
                if (editingException == null) onAdd(exception) else onEdit(exception)
                showDialog = false
                editingException = null
            },
            onDismiss = {
                showDialog = false
                editingException = null
            }
        )
    }
}

@Composable
fun ExceptionItem(
    exception: CategorizationException,
    groups: List<CategoryGroup>,
    onEdit: (CategorizationException) -> Unit,
    onDelete: () -> Unit
) {
    val groupName = groups.firstOrNull { it.id == exception.group_id }?.description ?: "?"
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exception.value1 ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${exception.month} → $groupName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onEdit(exception) }) {
                Icon(Icons.Filled.Edit, contentDescription = null)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = null)
            }
        }
    }
}

@Composable
fun ExceptionDialog(
    exception: CategorizationException?,
    groups: List<CategoryGroup>,
    onConfirm: (CategorizationException) -> Unit,
    onDismiss: () -> Unit
) {
    var month by remember { mutableStateOf(exception?.month ?: "") }
    var groupId by remember { mutableStateOf(exception?.group_id ?: 0) }
    var value1 by remember { mutableStateOf(exception?.value1 ?: "") }
    var value2 by remember { mutableStateOf(exception?.value2 ?: "") }
    var showGroupPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (exception == null) R.string.categories_add_exception
                    else R.string.categories_edit_exception
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = month,
                    onValueChange = { month = it },
                    label = { Text("Mes (YYYYMM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Grupo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { showGroupPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = groups.firstOrNull { it.id == groupId }?.description
                            ?: "Selecciona grupo"
                    )
                }

                OutlinedTextField(
                    value = value1,
                    onValueChange = { value1 = it },
                    label = { Text("Concepto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = value2,
                    onValueChange = { value2 = it },
                    label = { Text("Valor 2 (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        CategorizationException(
                            id = exception?.id,
                            month = month,
                            group_id = groupId,
                            value1 = value1.takeIf { it.isNotBlank() },
                            value2 = value2.takeIf { it.isNotBlank() }
                        )
                    )
                },
                enabled = month.isNotBlank() && value1.isNotBlank() && groupId > 0
            ) {
                Text(stringResource(R.string.dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )

    if (showGroupPicker) {
        AlertDialog(
            onDismissRequest = { showGroupPicker = false },
            title = { Text("Selecciona grupo") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    groups.forEach { g ->
                        TextButton(
                            onClick = {
                                groupId = g.id ?: 0
                                showGroupPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(g.description)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}