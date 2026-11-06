#!/usr/bin/env python3
"""
Rewrites the KDM json produced by this branch (the committed `KdFragments` model) into the shape of the
real `KdBundle` model — the `kotlin-documentation-model` prototype vendored under
src/main/kotlin/org/jetbrains/kotlin/documentation (KEEP #484) — so the page-set prototype can read
the ui-showcase modules until Dokka emits the real model itself.

Usage: ./migrate-model.py [path to ui-showcase-result]
Writes models/jvm/model.json and models/kmp/model.json next to this script.

What changes:
  * root gets a synthetic `schemaVersion` and `expectActualMappings`                        [KdBundle]
  * `fragment.elements` is a map keyed by id, `id` fields are gone, `embeddedElements` added
  * kinds: `variable` becomes `property` — the model has no separate enum-entry or field kinds;
    `classKind` values pass through, they are already the model's enum names
  * containers: constructors+callables+classlikes -> `declarations`
  * `returns` is gone: `returnType` on functions, `type` on properties; an enum entry already
    carries its enum class there in the input
  * `receiverParameter` -> `extensionReceiverParameter`, `defaultValue` -> `defaultValueExpression`
  * annotations: `useSiteTargets` dropped, arguments become a map, `classlike` -> `classLikeValue`,
    const values become `KdConstExpression`s; `@SinceKotlin` -> `since`, `@Deprecated` -> `deprecated`
  * types: `nullability` -> `isMarkedNullable`, `typeArguments` -> `qualifiers` (each with its own
    `typeArguments`), projections `{"type", "variance"}` -> the polymorphic invariant/in/out/star,
    type-parameter types get `containerElementId`
  * every field the model requires is written, even when the input omits it (defaults below)
  * `documentation` is dropped at the very end: KdDocumentation is an empty placeholder in the
    model, there is nothing to put in it yet

Inferred, because the input does not carry it (see the README):
  * expect/actual: the input has no `actuality`, so "differs from the parent fragment" is the signal;
    documentation is kept through the pipeline and stripped only at the end, because for some
    expect/actual pairs it is the only difference
  * the input re-emits some declarations of dependsOn fragments without documentation (an analyzer
    artifact of this branch); such copies are dropped when a documented twin exists
  * annotation const values have no type in the input; they are all written as strings
  * type arguments of `A.B<T>` are all put on the last qualifier
  * visibility is always PUBLIC, modality FINAL unless the input says otherwise, and flags absent
    from the input (isError, isLateinit, getter/setter, overridens, ...) are false/null/empty
"""
import copy
import json
import os
import sys
from collections import defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
MODULES = ("jvm", "kmp")
SCHEMA_VERSION = "0.0.0-migrated"

# element kinds of the real model that can be an actual of an expect in a parent fragment
DECLARATION_KINDS = {"class", "typealias", "constructor", "function", "property"}

PROJECTION_KINDS = {"INVARIANT": "invariant", "IN": "in", "OUT": "out", "STAR": "star"}
TYPE_VARIANCE = {"IN": "IN", "OUT": "OUT", "INVARIANT": None}


def fail(message):
    raise SystemExit(f"migrate-model: {message}")


def take(source, *keys):
    """Copies the present `keys` of `source` in order; used for fields whose shape is unchanged."""
    return {key: source[key] for key in keys if key in source}


def check_keys(element, known):
    unknown = set(element) - set(known)
    if unknown:
        fail(f"unhandled keys {sorted(unknown)} in {element.get('kind')} {element.get('id')}")


# ids look like `CALLABLE:package/Outer.Inner/name/hash` and `CLASS:package/Outer.Inner`

def callable_parts(element_id):
    package, class_names, name, hash_ = element_id.split(":", 1)[1].split("/")
    return package, class_names, name, hash_


def callable_owner(element_id):
    package, class_names, _, _ = callable_parts(element_id)
    return f"CLASS:{package}/{class_names}" if class_names else None


def class_names(classlike_id):
    return classlike_id.split(":", 1)[1].split("/", 1)[1].split(".")


# types, annotations, parameters

def migrate_projection(projection):
    """The input's `{"type", "variance"}` -> the model's polymorphic invariant/in/out/star."""
    result = {"kind": PROJECTION_KINDS.get(projection.get("variance"), "invariant")}
    if "type" in projection:
        result["type"] = migrate_type(projection["type"])
    return result


def migrate_type(type_):
    kind = type_["kind"]
    if kind == "classlike":
        check_keys(type_, ("kind", "classLikeId", "typeArguments", "nullability", "annotations"))
        qualifiers = [{"name": name, "typeArguments": []} for name in class_names(type_["classLikeId"])]
        for projection in type_.get("typeArguments", []):
            qualifiers[-1]["typeArguments"].append(migrate_projection(projection))
        result = {"kind": "classLike", "qualifiers": qualifiers, "classLikeId": type_["classLikeId"],
                  "isMarkedNullable": type_.get("nullability") == "NULLABLE"}
    elif kind == "functional":
        check_keys(type_, ("kind", "returnType", "receiverType", "valueParameterTypes",
                           "contextParameterTypes", "isSuspend", "nullability", "annotations"))
        result = {
            "kind": kind,
            "returnType": migrate_projection(type_["returnType"]),
            "receiverType": migrate_projection(type_["receiverType"]) if "receiverType" in type_ else None,
            "valueParameterTypes": [migrate_projection(p) for p in type_.get("valueParameterTypes", [])],
            "contextParameterTypes": [migrate_projection(p) for p in type_.get("contextParameterTypes", [])],
            "isSuspend": type_.get("isSuspend", False),
            "isMarkedNullable": type_.get("nullability") == "NULLABLE",
        }
    elif kind == "typeParameter":
        check_keys(type_, ("kind", "name", "nullability", "annotations"))
        # `containerElementId` is filled in by `resolve_type_parameters`, it needs the whole fragment
        result = {"kind": kind, "name": type_["name"], "containerElementId": None,
                  "isMarkedNullable": type_.get("nullability") == "NULLABLE"}
    elif kind == "flexible":
        return {"kind": kind, "lowerBound": migrate_type(type_["lowerBound"]),
                "upperBound": migrate_type(type_["upperBound"])}
    elif kind == "dynamic":
        return {"kind": kind}
    else:
        fail(f"unknown type kind {kind}")
    result["annotations"] = [migrate_annotation(a) for a in type_.get("annotations", [])]
    return result


def migrate_argument_value(value):
    kind = value["kind"]
    if kind == "const":
        # the input has no type for const values; all are written as strings
        const = {"value": {"kind": "string", "value": str(value["value"]["value"])}, "expression": None}
        return {"kind": "const", "expression": const}
    if kind == "enum":
        return {"kind": "enumEntry", "enumEntryId": value["callableId"]}
    if kind == "annotation":
        return {"kind": "annotation", "annotation": migrate_annotation(value["annotation"])}
    if kind == "array":
        return {"kind": "array", "elements": [migrate_argument_value(v) for v in value["elements"]]}
    if kind == "classlike":
        return {"kind": "classLikeValue", "classLikeId": value["classLikeId"]}
    fail(f"unknown annotation argument kind {kind}")


def migrate_annotation(annotation):
    """The model has no `useSiteTargets`: an annotated declaration is its own use site."""
    return {
        "classLikeId": annotation["classLikeId"],
        "arguments": {a["name"]: migrate_argument_value(a["value"]) for a in annotation.get("arguments", [])},
    }


def const_string(argument_value):
    return argument_value["value"]["value"]


def migrate_deprecated(annotation):
    arguments = {a["name"]: a["value"] for a in annotation.get("arguments", [])}
    level = "WARNING"
    if "level" in arguments:
        level = callable_parts(arguments["level"]["callableId"])[2]
    if level == "HIDDEN":
        fail("DeprecationLevel.HIDDEN should be filtered out before it reaches KDM")
    # `message` is KdDocumentation, an empty placeholder in the model
    result = {"since": None, "message": {}, "level": level}
    if "replaceWith" in arguments:
        replace_with = {a["name"]: a["value"] for a in arguments["replaceWith"]["annotation"]["arguments"]}
        imports = [const_string(v) for v in replace_with.get("imports", {}).get("elements", [])]
        result["replaceWith"] = {"imports": imports,
                                 "expression": {"text": const_string(replace_with["expression"]), "links": []}}
    return result


def migrate_annotations_into(result, source):
    """`@SinceKotlin` and `@Deprecated` are concepts of their own in the model, not annotations."""
    annotations = []
    for annotation in source.get("annotations", []):
        if annotation["classLikeId"] == "CLASS:kotlin/SinceKotlin":
            result["since"] = const_string(annotation["arguments"][0]["value"])
        elif annotation["classLikeId"] == "CLASS:kotlin/Deprecated":
            result["deprecated"] = migrate_deprecated(annotation)
        else:
            annotations.append(migrate_annotation(annotation))
    result["annotations"] = annotations


def migrate_type_parameter(parameter):
    check_keys(parameter, ("name", "isReified", "upperBounds", "variance", "annotations", "documentation"))
    result = {
        "name": parameter["name"],
        "isReified": parameter.get("isReified", False),
        "upperBounds": [migrate_type(t) for t in parameter.get("upperBounds", [])],
        "variance": TYPE_VARIANCE.get(parameter.get("variance")),
        "annotations": [migrate_annotation(a) for a in parameter.get("annotations", [])],
    }
    result.update(take(parameter, "documentation"))
    return result


def migrate_value_parameter(parameter):
    check_keys(parameter, ("name", "type", "isNoinline", "isCrossinline", "isVararg", "defaultValue",
                           "annotations", "documentation"))
    default = None
    if "defaultValue" in parameter:
        raw = parameter["defaultValue"]
        if raw["kind"] != "const":
            fail(f"unknown default value kind {raw['kind']}")
        # the input stores the source text of the default value, which is exactly `KdExpression.text`
        default = {"kind": "defined", "expression": {"text": raw["value"]["value"], "links": []}}
    result = {
        "name": parameter["name"],
        "type": migrate_type(parameter["type"]),
        "isNoinline": parameter.get("isNoinline", False),
        "isCrossinline": parameter.get("isCrossinline", False),
        "isVararg": parameter.get("isVararg", False),
        "defaultValueExpression": default,
        "annotations": [migrate_annotation(a) for a in parameter.get("annotations", [])],
    }
    result.update(take(parameter, "documentation"))
    return result


def migrate_extension_receiver(parameter):
    result = {
        "type": migrate_type(parameter["type"]),
        "annotations": [migrate_annotation(a) for a in parameter.get("annotations", [])],
    }
    result.update(take(parameter, "documentation"))
    return result


def migrate_context_parameter(parameter):
    result = {
        "name": parameter.get("name"),
        "type": migrate_type(parameter["type"]),
        "annotations": [migrate_annotation(a) for a in parameter.get("annotations", [])],
    }
    result.update(take(parameter, "documentation"))
    return result


def documentation_with_return_tag(element):
    documentation = list(element.get("documentation", []))
    return_documentation = element.get("returns", {}).get("documentation")
    if return_documentation:
        documentation.append({"kind": "tag", "name": "return", "children": return_documentation})
    return documentation


def migrate_tail_into(result, element, throws):
    """Fills the fields every declaration shares: annotations, `since`, `deprecated`, `throws`."""
    migrate_annotations_into(result, element)
    if throws:
        # items keep their `classLikeId`; their `documentation` is dropped at the end
        result["throws"] = element.get("throws", [])
    documentation = documentation_with_return_tag(element)
    if documentation or "documentation" in element:
        result["documentation"] = documentation


# elements

def migrate_element(element, declarations=None):
    kind = element["kind"]
    if kind == "class":
        check_keys(element, ("kind", "id", "name", "classKind", "isCompanion", "isData", "isValue", "isFun",
                             "isInner", "isExternal", "superTypes", "constructors", "callables", "classlikes",
                             "visibility", "modality", "annotations", "typeParameters", "documentation"))
        result = {
            "kind": kind,
            "name": element["name"],
            "classKind": element["classKind"],
            "isCompanion": element.get("isCompanion", False),
            "isData": element.get("isData", False),
            "isValue": element.get("isValue", False),
            "isFun": element.get("isFun", False),
            "isInner": element.get("isInner", False),
            "isExternal": element.get("isExternal", False),
            "isError": False,  # the input has no rich-errors information
            "mappedTo": None,
            "typeParameters": [migrate_type_parameter(p) for p in element.get("typeParameters", [])],
            "superTypes": [migrate_type(t) for t in element.get("superTypes", [])],
            "declarations": declarations or [],
            "visibility": "PUBLIC",  # the input does not carry visibility
            "modality": element.get("modality", "FINAL"),
            "since": None,
            "deprecated": None,
            "annotations": [],
        }
        migrate_tail_into(result, element, throws=False)
    elif kind == "typealias":
        check_keys(element, ("kind", "id", "name", "underlyingType", "visibility", "annotations",
                             "typeParameters", "documentation"))
        result = {
            "kind": kind,
            "name": element["name"],
            "typeParameters": [migrate_type_parameter(p) for p in element.get("typeParameters", [])],
            "underlyingType": migrate_type(element["underlyingType"]),
            "declarations": declarations or [],  # the input has none; an `actual typealias` would
            "visibility": "PUBLIC",
            "since": None,
            "deprecated": None,
            "annotations": [],
        }
        migrate_tail_into(result, element, throws=False)
    elif kind == "constructor":
        check_keys(element, ("kind", "id", "name", "returns", "isPrimary", "isExternal", "valueParameters",
                             "typeParameters", "visibility", "annotations", "throws", "documentation"))
        result = {
            "kind": kind,
            "isPrimary": element.get("isPrimary", False),
            "isExternal": element.get("isExternal", False),
            "mappedTo": None,
            "typeParameters": [migrate_type_parameter(p) for p in element.get("typeParameters", [])],
            "valueParameters": [migrate_value_parameter(p) for p in element.get("valueParameters", [])],
            "visibility": "PUBLIC",
            "since": None,
            "deprecated": None,
            "throws": [],
            "annotations": [],
        }
        migrate_tail_into(result, element, throws=True)
    elif kind == "function":
        check_keys(element, ("kind", "id", "name", "returns", "isCompanion", "isExternal", "isSuspend",
                             "isOperator", "isInfix", "isInline", "isTailRec", "typeParameters",
                             "valueParameters", "receiverParameter", "contextParameters", "visibility",
                             "modality", "annotations", "throws", "documentation"))
        result = {
            "kind": kind,
            "name": element["name"],
            "isCompanion": element.get("isCompanion", False),
            "isExternal": element.get("isExternal", False),
            "isSuspend": element.get("isSuspend", False),
            "isOperator": element.get("isOperator", False),
            "isInfix": element.get("isInfix", False),
            "isInline": element.get("isInline", False),
            "isTailRec": element.get("isTailRec", False),
            "mappedTo": None,
            "returnType": migrate_type(element["returns"]["type"]),
            "typeParameters": [migrate_type_parameter(p) for p in element.get("typeParameters", [])],
            "valueParameters": [migrate_value_parameter(p) for p in element.get("valueParameters", [])],
            "extensionReceiverParameter": (migrate_extension_receiver(element["receiverParameter"])
                                           if element.get("receiverParameter") else None),
            "contextParameters": [migrate_context_parameter(p) for p in element.get("contextParameters", [])],
            "overridens": [],  # the input does not carry overrides
            "visibility": "PUBLIC",
            "modality": element.get("modality", "FINAL"),
            "since": None,
            "deprecated": None,
            "throws": [],
            "annotations": [],
        }
        migrate_tail_into(result, element, throws=True)
    elif kind == "variable":
        check_keys(element, ("kind", "id", "name", "returns", "variableKind", "isMutable", "isCompanion",
                             "isExternal", "constValue", "receiverParameter", "contextParameters",
                             "typeParameters", "visibility", "modality", "annotations", "throws",
                             "documentation"))
        result = {
            "kind": "property",
            "name": element["name"],
            "isLateinit": False,  # not in the input
            "isInline": False,
            "isCompanion": element.get("isCompanion", False),
            "isExternal": element.get("isExternal", False),
            "isSuspend": False,
            "isMutable": element.get("isMutable", False),
            "isFromPrimaryConstructor": False,
            "constExpression": None,  # `constValue` never appears in the input
            "getter": None,
            "setter": None,
            "mappedTo": None,
            "type": migrate_type(element["returns"]["type"]),  # for an enum entry, its enum class
            "typeParameters": [migrate_type_parameter(p) for p in element.get("typeParameters", [])],
            "extensionReceiverParameter": (migrate_extension_receiver(element["receiverParameter"])
                                           if element.get("receiverParameter") else None),
            "contextParameters": [migrate_context_parameter(p) for p in element.get("contextParameters", [])],
            "overridens": [],
            "visibility": "PUBLIC",
            "modality": element.get("modality", "FINAL"),
            "since": None,
            "deprecated": None,
            "throws": [],
            "annotations": [],
        }
        migrate_tail_into(result, element, throws=True)
    elif kind == "package":
        check_keys(element, ("kind", "id", "name", "classlikes", "callables", "annotations", "documentation"))
        result = {
            "kind": kind,
            "name": element["name"],
            "declarations": declarations or [],
            "annotations": [migrate_annotation(a) for a in element.get("annotations", [])],
        }
        result.update(take(element, "documentation"))
    elif kind == "module":
        check_keys(element, ("kind", "id", "name", "packages", "annotations", "documentation"))
        result = {
            "kind": kind,
            "name": element["name"],
            "packages": element.get("packages", []),
            "annotations": [migrate_annotation(a) for a in element.get("annotations", [])],
        }
        result.update(take(element, "documentation"))
    else:
        fail(f"unknown element kind {kind}")
    return result


def container_declarations(element):
    return element.get("constructors", []) + element.get("callables", []) + element.get("classlikes", [])


# fragment-level passes

def assign_inherited_copies(raw_elements):
    """
    The input stores an inherited member under the id of the declaring class (`kotlin/Any/equals`), once
    per inheriting class, emitted right before that class. Gives every copy the id of its owner.
    Returns {(owner class id, original id): (new id, raw copy)}.
    """
    copies_by_id = defaultdict(list)  # id -> [(position, element)], unassigned copies
    assigned = {}
    for position, element in enumerate(raw_elements):
        if element["kind"] != "class":
            copies_by_id[element["id"]].append((position, element))
            continue
        owner_id = element["id"]
        owner_names = owner_id.split(":", 1)[1]
        for member_id in element.get("callables", []):
            package, names, name, hash_ = callable_parts(member_id)
            if f"{package}/{names}" == owner_names:
                continue
            candidates = copies_by_id[member_id]
            if not candidates:
                fail(f"no copy of inherited {member_id} before {owner_id}")
            # the latest unassigned copy before the class is the one emitted for it
            _, copy_ = candidates.pop()
            assigned[(owner_id, member_id)] = (f"CALLABLE:{owner_names}/{name}/{hash_}", copy_)
    for (owner_id, member_id), (_, copy_) in assigned.items():
        mentioned = {t["classLikeId"] for t in walk_dicts(copy_) if t.get("kind") == "classlike"}
        foreign_owners = {o for (o, m) in assigned if m == member_id and o != owner_id}
        if mentioned & foreign_owners:
            fail(f"copy of {member_id} assigned to {owner_id} mentions another owner")
    return assigned


def drop_undocumented_twins(copies):
    """Copies that equal a documented copy with its documentation removed are analyzer artifacts."""
    documented = [c for c in copies if c.get("documentation")]
    stripped = [{k: v for k, v in c.items() if k != "documentation"} for c in documented]
    return [c for c in copies if c.get("documentation") or c not in stripped]


def migrate_fragment(raw_fragment):
    raw_elements = raw_fragment["elements"]
    inherited = assign_inherited_copies(raw_elements)
    inherited_ids = {member_id for (_, member_id) in inherited}
    own_ids = {e["id"] for e in raw_elements if e["id"] not in inherited_ids}

    elements = {}
    embedded = {}
    stats = defaultdict(int)

    for member_id in sorted(inherited_ids):
        owners = [(o, new_id, c) for (o, m), (new_id, c) in inherited.items() if m == member_id]
        identical = all(c == owners[0][2] for _, _, c in owners)
        for owner_id, new_id, copy_ in owners:
            if identical:
                elements[new_id] = {"kind": "inheritedMember", "originalId": member_id}
                stats["inheritedMember"] += 1
            else:
                # substituted signature (`Enum<E>.compareTo(E)` in `enum class A`) - nothing to share
                elements[new_id] = migrate_element(copy_)
                stats["materialized inherited"] += 1
        if identical and member_id not in own_ids:
            embedded[member_id] = migrate_element(owners[0][2])

    copies_by_id = defaultdict(list)
    for element in raw_elements:
        if element["id"] not in inherited_ids:
            copies_by_id[element["id"]].append(element)

    candidates = {}  # id -> [raw copies]; more than one means expect + actual, resolved by `migrate_model`
    for element_id, copies in copies_by_id.items():
        copies = drop_undocumented_twins(copies)
        stats["dropped undocumented twins"] += len(copies_by_id[element_id]) - len(copies)
        candidates[element_id] = copies

    def remap(ids, owner_id):
        return [inherited[(owner_id, i)][0] if (owner_id, i) in inherited else i for i in ids]

    def migrate_candidate(element):
        declarations = remap(container_declarations(element), element["id"])
        return migrate_element(element, declarations)

    migrated_candidates = {i: [migrate_candidate(c) for c in copies] for i, copies in candidates.items()}
    return elements, embedded, migrated_candidates, stats


def walk_dicts(value):
    if isinstance(value, dict):
        yield value
        for child in value.values():
            yield from walk_dicts(child)
    elif isinstance(value, list):
        for child in value:
            yield from walk_dicts(child)


def resolve_type_parameters(elements, embedded, stats, extra=()):
    """
    Sets `containerElementId` on type-parameter types: the declaration itself, then its outer classes.
    `extra` are (id, element) pairs not in `elements` yet, resolved in the same scope.
    """
    owner_of = {}
    for container_id, element in elements.items():
        for declaration_id in element.get("declarations", []):
            owner_of[declaration_id] = container_id

    def scope(element_id, element):
        while element_id:
            yield element_id, element
            element_id = owner_of.get(element_id) or (
                callable_owner(element_id) if element_id.startswith("CALLABLE:") else None)
            element = elements.get(element_id) or embedded.get(element_id) or {}

    pairs = list(elements.items()) + list(embedded.items()) + list(extra)
    for element_id, element in pairs:
        for type_ in walk_dicts(element):
            if type_.get("kind") != "typeParameter" or type_.get("containerElementId") is not None:
                continue
            container = next((i for i, e in scope(element_id, element)
                              if any(p["name"] == type_["name"] for p in e.get("typeParameters", []))), None)
            if container:
                type_["containerElementId"] = container
            else:
                # keep the required field non-null rather than dropping the declaration
                stats["unresolved type parameters"] += 1
                type_["containerElementId"] = element_id


def migrate_model(raw):
    raw_fragments = raw["fragments"]
    parents = {f["name"]: [d["name"] for d in f.get("fragmentDependencies", [])] for f in raw_fragments}
    stats = defaultdict(int)
    migrated = {}
    for raw_fragment in raw_fragments:
        elements, embedded, candidates, fragment_stats = migrate_fragment(raw_fragment)
        for key, value in fragment_stats.items():
            stats[key] += value
        migrated[raw_fragment["name"]] = (elements, embedded, candidates)

    # `resolved[fragment][id]` = (fragment the content comes from, content), after refinement
    resolved = {}
    mappings = {}  # id of the expect -> {"expectIds": {...}, "actualIds": {...}}

    def parent_content(fragment_name, element_id):
        for parent in parents[fragment_name]:
            if element_id in resolved[parent]:
                return resolved[parent][element_id]
        return None

    def declared_content(fragment_name, element_id):
        """(fragment, content) of the topmost dependsOn fragment that has `element_id` - the expect."""
        found = None
        for parent in parents[fragment_name]:
            if element_id in resolved[parent]:
                found = declared_content(parent, element_id) or resolved[parent][element_id]
                break
        return found

    def ordered(names):
        done = []
        while len(done) < len(names):
            ready = [n for n in names if n not in done and all(p in done for p in parents[n])]
            if not ready:
                fail("cycle in fragment dependencies")
            done.extend(ready)
        return done

    fragments = []
    for fragment_name in ordered([f["name"] for f in raw_fragments]):
        elements, embedded, candidates = migrated[fragment_name]
        for element_id, copies in candidates.items():
            if len(copies) == 1:
                elements[element_id] = copies[0]
        resolve_type_parameters(elements, embedded, stats,
                                [(i, c) for i, copies in candidates.items() if len(copies) > 1 for c in copies])
        for element_id, copies in candidates.items():
            if len(copies) == 1:
                elements[element_id] = copies[0]
                continue
            # both the expect and an actual are visible here (`linuxArm64Main` sees the `commonMain`
            # expect and the `linuxMain` actual); the actual is the one that is not the expect
            expect = declared_content(fragment_name, element_id)
            own = [c for c in copies if expect is None or c != expect[1]]
            if len(own) != 1:
                fail(f"{fragment_name}: cannot tell expect from actual for {element_id} ({len(copies)} copies)")
            elements[element_id] = own[0]

        resolved[fragment_name] = {}
        for element_id, element in elements.items():
            from_parent = parent_content(fragment_name, element_id)
            if from_parent and from_parent[1] == element:
                resolved[fragment_name][element_id] = from_parent
                elements[element_id] = {"kind": "refinedElement", "fragmentName": from_parent[0]}
                stats["refinedElement"] += 1
                continue
            resolved[fragment_name][element_id] = (fragment_name, element)
            if from_parent and element["kind"] in DECLARATION_KINDS:
                mapping = mappings.setdefault(element_id, {"expectIds": {}, "actualIds": {}})
                mapping["expectIds"][declared_content(fragment_name, element_id)[0]] = element_id
                mapping["actualIds"][fragment_name] = element_id

        fragments.append({"name": fragment_name, "elements": elements, "embeddedElements": embedded})
        stats["embeddedElements"] += len(embedded)

    order = [f["name"] for f in raw_fragments]
    fragments.sort(key=lambda f: order.index(f["name"]))
    stats["expectActualMappings"] = len(mappings)
    return {"schemaVersion": SCHEMA_VERSION, "fragments": fragments,
            "expectActualMappings": list(mappings.values())}, stats


def strip_documentation(value):
    """Drops `documentation` everywhere: KdDocumentation is an empty placeholder in the model."""
    if isinstance(value, dict):
        return {k: strip_documentation(v) for k, v in value.items() if k != "documentation"}
    if isinstance(value, list):
        return [strip_documentation(v) for v in value]
    return value


def check_references(model):
    dangling = 0
    for fragment in model["fragments"]:
        for element in fragment["elements"].values():
            for reference in element.get("declarations", []) + element.get("packages", []):
                dangling += reference not in fragment["elements"]
            if element["kind"] == "inheritedMember":
                original = element["originalId"]
                dangling += original not in fragment["elements"] and original not in fragment["embeddedElements"]
    return dangling


def main():
    root = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
        HERE, "../../dokka-integration-tests/gradle/build/ui-showcase-result")
    for module in MODULES:
        source = os.path.join(root, module, "kdm", "model.json")
        with open(source) as f:
            raw = json.load(f)
        model, stats = migrate_model(copy.deepcopy(raw))
        model = strip_documentation(model)
        dangling = check_references(model)
        if dangling:
            fail(f"{module}: {dangling} dangling references")
        target = os.path.join(HERE, "models", module, "model.json")
        os.makedirs(os.path.dirname(target), exist_ok=True)
        with open(target, "w") as f:
            json.dump(model, f, indent=2, ensure_ascii=False)
            f.write("\n")
        before = sum(len(fr["elements"]) for fr in raw["fragments"])
        after = sum(len(fr["elements"]) for fr in model["fragments"])
        summary = ", ".join(f"{k}: {v}" for k, v in sorted(stats.items()) if v)
        print(f"{module}: {before} -> {after} elements ({os.path.getsize(source)} -> "
              f"{os.path.getsize(target)} bytes pretty) -> {os.path.relpath(target)}\n  {summary}")


if __name__ == "__main__":
    main()
